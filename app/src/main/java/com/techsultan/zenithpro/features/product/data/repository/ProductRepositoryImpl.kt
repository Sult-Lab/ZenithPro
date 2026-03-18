package com.techsultan.zenithpro.features.product.data.repository

import android.net.Uri
import android.util.Log
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.ImageUploadManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.product.data.local.ProductDao
import com.techsultan.zenithpro.features.product.data.local.ProductEntity
import com.techsultan.zenithpro.features.product.data.local.ProductStockDao
import com.techsultan.zenithpro.features.product.data.local.ProductStockEntity
import com.techsultan.zenithpro.features.product.data.local.ProductVariantDao
import com.techsultan.zenithpro.features.product.data.local.ProductVariantEntity
import com.techsultan.zenithpro.features.product.data.local.VariantAttributeEntity
import com.techsultan.zenithpro.features.product.data.mapper.toProductDto
import com.techsultan.zenithpro.features.product.data.mapper.toProductEntity
import com.techsultan.zenithpro.features.product.data.remote.AddProductRequest
import com.techsultan.zenithpro.features.product.data.remote.CreateVariantsRequest
import com.techsultan.zenithpro.features.product.data.remote.ProductDto
import com.techsultan.zenithpro.features.product.data.remote.ProductVariantCreate
import com.techsultan.zenithpro.features.product.data.remote.ProductVariantCreateRequest
import com.techsultan.zenithpro.features.product.data.remote.StockCreateRequest
import com.techsultan.zenithpro.features.product.data.remote.VariantAttributeInput
import com.techsultan.zenithpro.features.product.domain.repository.ProductRepository
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

class ProductRepositoryImpl(
    private val imageUploadManager: ImageUploadManager,
    private val postgrest: Postgrest,
    private val functions: Functions,
    private val networkMonitor: NetworkMonitor,
    private val productDao: ProductDao,
    private val variantDao: ProductVariantDao,
    private val stockDao: ProductStockDao,
) : ProductRepository {

    override suspend fun addProduct(
        productRequest: AddProductRequest,
        imageUris: List<Uri>,
    ): Resource<Unit> {
        Log.d("ProductRepo", "addProduct: Starting for product: ${productRequest.name}")
        return try {

            val productId = UUID.randomUUID().toString()
            val now = Instant.now().toString()

            Log.d("ProductRepo", "addProduct: Inserting product $productId locally")
            productDao.insertProduct(
                ProductEntity(
                    id = productId,
                    businessId = productRequest.businessId,
                    name = productRequest.name,
                    description = productRequest.description,
                    category = productRequest.category,
                    baseSalesPrice = productRequest.baseSalesPrice,
                    baseCostPrice = productRequest.baseCostPrice,
                    imageUrl = null,
                    isActive = productRequest.isActive,
                    imageUrls = imageUris.map { it.toString() },
                    expiryWarningDays = productRequest.expiryWarningDays,
                    updatedAt = now,
                    deletedAt = null,
                    syncStatus = Util.SyncStatus.PENDING,
                    locallyCreatedAt = now
                )
            )
            productRequest.variants?.forEach { variantReq ->
                val variantId = UUID.randomUUID().toString()
                Log.d("ProductRepo", "addProduct: Inserting variant $variantId")
                variantDao.insertVariant(
                    ProductVariantEntity(
                        id = variantId,
                        productId = productId,
                        businessId = productRequest.businessId,
                        sku = variantReq.sku,
                        salesPrice = variantReq.salesPrice,
                        costPrice = variantReq.costPrice,
                        barcode = variantReq.barcode,
                        updatedAt = now,
                        deletedAt = null,
                        syncStatus = Util.SyncStatus.PENDING
                    )
                )
                variantDao.insertAttributes(
                    variantReq.attributes.map {
                        VariantAttributeEntity(
                            id = UUID.randomUUID().toString(),
                            variantId = variantId,
                            optionName = it.optionName,
                            optionValue = it.optionValue
                        )
                    }
                )
                stockDao.insertStock(
                    variantReq.stock.map {
                        ProductStockEntity(
                            id = UUID.randomUUID().toString(),
                            variantId = variantId,
                            quantity = it.quantity,
                            expiryDate = it.expiryDate,
                            lowStockAlert = it.lowStockAlert,
                            updatedAt = now,
                            syncStatus = Util.SyncStatus.PENDING
                        )
                    }
                )
            }

            val imagePaths: List<String> = if (imageUris.isNotEmpty()) {
                Log.d("ProductRepo", "addProduct: Uploading ${imageUris.size} images")
                imageUploadManager.uploadProductImages(imageUris, productRequest.businessId)
            } else {
                Log.d("ProductRepo", "addProduct: No images to upload")
                emptyList()
            }
            Log.d("ProductRepo", "addProduct: Image paths: $imagePaths")

            val isConnected = networkMonitor.isConnected()
            Log.d("ProductRepo", "addProduct: Network connection: $isConnected")
            if (isConnected) {
                Log.d("ProductRepo", "addProduct: Network connected, calling pushNewProduct")
                pushNewProduct(productId, imageUris, productRequest)
            } else {
                Log.i("ProductRepo", "addProduct: Offline. Product $productId saved locally as PENDING")
            }

            Resource.Success(Unit)

        } catch (e: Exception) {
            Log.e("ProductRepo", "addProduct error: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to add product")
        }
    }

    suspend fun pushNewProduct(
        productId: String,
        imageUris: List<Uri>,
        originalRequest: AddProductRequest,
    ) {
        Log.d("ProductRepo", "pushNewProduct: Starting sync for $productId")
        try {
            val productWithVariants = productDao.getProductWithVariants(productId)
            if (productWithVariants == null) {
                Log.e("ProductRepo", "pushNewProduct: Product $productId not found in local DB")
                return
            }

            val imagePaths = if (imageUris.isNotEmpty()) {
                Log.d("ProductRepo", "pushNewProduct: Uploading/verifying images for sync")
                imageUploadManager.uploadProductImages(imageUris, productWithVariants.product.businessId)
            } else {
                productWithVariants.product.imageUrls.filterNot { it.startsWith("content://") }
            }
            Log.d("ProductRepo", "pushNewProduct: Image paths for remote: $imagePaths")

            // Build request using the locally-stored IDs as clientId
            val syncRequest = AddProductRequest(
                clientId = productId,
                name = productWithVariants.product.name,
                description = productWithVariants.product.description,
                category = productWithVariants.product.category,
                baseSalesPrice = productWithVariants.product.baseSalesPrice,
                baseCostPrice = productWithVariants.product.baseCostPrice,
                expiryWarningDays = productWithVariants.product.expiryWarningDays,
                isActive = productWithVariants.product.isActive,
                businessId = productWithVariants.product.businessId,
                imageUrls = imagePaths,
                variants = productWithVariants.variants.map { variantWithStock ->
                    ProductVariantCreateRequest(
                        clientId = variantWithStock.variant.id,
                        sku = variantWithStock.variant.sku,
                        salesPrice = variantWithStock.variant.salesPrice,
                        costPrice = variantWithStock.variant.costPrice,
                        barcode = variantWithStock.variant.barcode,
                        attributes = variantWithStock.attributes.map {
                            VariantAttributeInput(it.optionName, it.optionValue)
                        },
                        stock = variantWithStock.stock.map {
                            StockCreateRequest(it.quantity, it.expiryDate, it.lowStockAlert)
                        }
                    )
                }
            )

            Log.d("ProductRepo", "pushNewProduct: Invoking edge function 'create_product_with_variants'")
            val response = functions.invoke(
                function = "add_product",
                body = syncRequest
            )
            Log.d("ProductRepo", "pushNewProduct: Edge function response status: ${response.status}")

            // Update Room with the server's canonical updatedAt timestamp
            val responseBody = response.body<SyncResponse>()
            val updatedAt = responseBody.updatedAt
            Log.d("ProductRepo", "pushNewProduct: Success. Server updatedAt: $updatedAt")
            
            productDao.markSynced(productId, updatedAt)
            productWithVariants.variants.forEach {
                variantDao.markSynced(it.variant.id, updatedAt)
            }

            // Persist real image URLs
            if (imagePaths.isNotEmpty()) {
                productDao.insertProduct(
                    productWithVariants.product.copy(
                        imageUrls = imagePaths,
                        syncStatus = Util.SyncStatus.SYNCED
                    )
                )
                Log.d("ProductRepo", "pushNewProduct: Updated product with remote image URLs")
            }
        } catch (e: Exception) {
            Log.e("ProductRepo", "pushNewProduct failed for $productId: ${e.message}", e)
        }
    }

    override suspend fun deleteProduct(productId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            Log.d("ProductRepo", "deleteProduct: Requested for $productId")
            try {
                val now = Instant.now().toString()
                productDao.softDelete(productId, now)
                
                val isConnected = networkMonitor.isConnected()
                Log.d("ProductRepo", "deleteProduct: isConnected: $isConnected")
                if (isConnected) {
                    pushDelete(productId)
                }
                Resource.Success(Unit)
            } catch (e: Exception) {
                Log.e("ProductRepo", "deleteProduct error: ${e.message}", e)
                Resource.Error(e.message ?: "Delete failed")
            }
        }

    suspend fun pushDelete(productId: String) {
        Log.d("ProductRepo", "pushDelete: Sending remote delete for $productId")
        try {
            postgrest.from("products")
                .update(mapOf("deleted_at" to Instant.now().toString())) {
                    filter { eq("id", productId) }
                }
            productDao.hardDelete(productId)
            Log.d("ProductRepo", "pushDelete: Success for $productId")
        } catch (e: Exception) {
            Log.e("ProductRepo", "pushDelete failed for $productId: ${e.message}", e)
        }
    }

    override suspend fun pullFromServer(businessId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            Log.d("ProductRepo", "pullFromServer: Starting for business $businessId")
            try {
                val remoteProducts = postgrest.from("products")
                    .select { filter { eq("business_id", businessId) } }
                    .decodeList<ProductDto>()

                Log.d("ProductRepo", "pullFromServer: Fetched ${remoteProducts.size} products")

                val localUnsynced = productDao.getUnsyncedProducts().associateBy { it.id }
                Log.d("ProductRepo", "pullFromServer: ${localUnsynced.size} unsynced products locally")

                val merged = remoteProducts.map { dto ->
                    localUnsynced[dto.id] ?: dto.toProductEntity()
                }
                productDao.deleteAll()
                productDao.insertProducts(merged)
                Log.d("ProductRepo", "pullFromServer: Success")
                Resource.Success(Unit)
            } catch (e: Exception) {
                Log.e("ProductRepo", "pullFromServer error: ${e.message}", e)
                Resource.Error(e.message ?: "Pull failed")
            }
        }

    private suspend fun pushUpdate(entity: ProductEntity) {
        Log.d("ProductRepo", "pushUpdate: Sending update for ${entity.id}")
        try {
            postgrest.from("products").update(entity.toProductDto()) {
                filter { eq("id", entity.id) }
            }
            productDao.markSynced(entity.id, Instant.now().toString())
            Log.d("ProductRepo", "pushUpdate: Success for ${entity.id}")
        } catch (e: Exception) {
            Log.e("ProductRepo", "pushUpdate failed for ${entity.id}: ${e.message}", e)
        }
    }
}

@Serializable
data class SyncResponse(
    @SerialName("productId") val productId: String,
    @SerialName("updatedAt") val updatedAt: String
)
