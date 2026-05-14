package com.techsultan.zenithpro.features.inventory.data.repository

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.ImageCacheManager
import com.techsultan.zenithpro.core.util.ImageUploadManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.inventory.data.local.ProductDao
import com.techsultan.zenithpro.features.inventory.data.local.ProductEntity
import com.techsultan.zenithpro.features.inventory.data.local.ProductStockDao
import com.techsultan.zenithpro.features.inventory.data.local.ProductStockEntity
import com.techsultan.zenithpro.features.inventory.data.local.ProductVariantDao
import com.techsultan.zenithpro.features.inventory.data.local.ProductVariantEntity
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.inventory.data.local.VariantAttributeEntity
import com.techsultan.zenithpro.features.inventory.data.mapper.toEntity
import com.techsultan.zenithpro.features.inventory.data.mapper.toProductDto
import com.techsultan.zenithpro.features.inventory.data.mapper.toProductEntity
import com.techsultan.zenithpro.features.inventory.data.remote.AddProductRequest
import com.techsultan.zenithpro.features.inventory.data.remote.ProductDto
import com.techsultan.zenithpro.features.inventory.data.remote.ProductStockDto
import com.techsultan.zenithpro.features.inventory.data.remote.ProductVariantCreateRequest
import com.techsultan.zenithpro.features.inventory.data.remote.ProductVariantDto
import com.techsultan.zenithpro.features.inventory.data.remote.StockCreateRequest
import com.techsultan.zenithpro.features.inventory.data.remote.VariantAttributeDto
import com.techsultan.zenithpro.features.inventory.data.remote.VariantAttributeInput
import com.techsultan.zenithpro.features.inventory.domain.repository.ProductRepository
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.Objects.isNull
import java.util.UUID

class ProductRepositoryImpl(
    private val imageUploadManager: ImageUploadManager,
    private val postgrest: Postgrest,
    private val functions: Functions,
    private val networkMonitor: NetworkMonitor,
    private val productDao: ProductDao,
    private val variantDao: ProductVariantDao,
    private val stockDao: ProductStockDao,
    private val imageCacheManager: ImageCacheManager
) : ProductRepository {

    override fun getProducts(businessId: String): Flow<Resource<List<ProductWithVariants>>> =
        productDao.getProductsForBusiness(businessId)
            .map<List<ProductWithVariants>, Resource<List<ProductWithVariants>>> {
                Resource.Success(it)
            }
            .catch { emit(Resource.Error(it.message ?: "Failed to load products")) }
            .onStart { emit(Resource.Loading()) }

    override suspend fun addProduct(
        productRequest: AddProductRequest,
        imageUris: List<Uri>,
    ): Resource<Unit> {
        Log.d("ProductRepo", "addProduct: Starting for product: ${productRequest.name}")
        return try {

            val productId = productRequest.clientId
            val now = Instant.now().toString()

            val imageUrls: List<String> = if (imageUris.isNotEmpty()) {
                Log.d("ProductRepo", "addProduct: Uploading ${imageUris.size} images")
                imageUploadManager.uploadProductImages(
                    imageUris = imageUris,
                    businessId = productRequest.businessId,
                    name = productRequest.name
                )
            } else {
                Log.d("ProductRepo", "addProduct: No images to upload")
                emptyList()
            }
            Log.d("ProductRepo", "addProduct: Uploaded image URLs: $imageUrls")

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
                    imageUrl = imageUrls.firstOrNull(),
                    isActive = productRequest.isActive,
                    imageUrls = imageUrls,
                    expiryWarningDays = productRequest.expiryWarningDays,
                    updatedAt = now,
                    deletedAt = null,
                    syncStatus = Util.SyncStatus.PENDING,
                    locallyCreatedAt = now
                )
            )
            productRequest.variants?.forEach { variantReq ->
                val variantId = variantReq.clientId
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

            val isConnected = networkMonitor.isConnected()
            Log.d("ProductRepo", "addProduct: Network connection: $isConnected")
            if (isConnected) {
                Log.d("ProductRepo", "addProduct: Network connected, calling pushNewProduct")
                pushNewProduct(productId)
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
    ) {
        Log.d("ProductRepo", "pushNewProduct: Starting sync for $productId")
        try {
            val productWithVariants = productDao.getProductWithVariants(productId)
            if (productWithVariants == null) {
                Log.e("ProductRepo", "pushNewProduct: Product $productId not found in local DB")
                return
            }

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
                imageUrls = productWithVariants.product.imageUrls,
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

            Log.d("ProductRepo", "pushNewProduct: Invoking edge function 'add_product'")
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

            productWithVariants.variants.forEach { variantWithStock ->
                variantDao.markSynced(variantWithStock.variant.id, updatedAt)

                variantWithStock.stock.forEach { stock ->
                    stockDao.markSynced(stock.id, responseBody.updatedAt)
                }

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
            
            if (businessId.isBlank()) {
                Log.e("ProductRepo", "pullFromServer: businessId is blank")
                return@withContext Resource.Error("Business ID is missing")
            }

            try {
                // 1. Snapshot local unsynced IDs — we never overwrite these
                val unsyncedProductIds = productDao.getUnsyncedProducts()
                    .map { it.id }.toSet()
                val unsyncedVariantIds = variantDao.getUnsyncedVariants()
                    .map { it.id }.toSet()

                // 2. Fetch remote products
                val remoteProducts = postgrest
                    .from("products")
                    .select {
                        filter {
                            eq("business_id", businessId)
                            isNull("deleted_at")
                        }
                    }
                    .decodeList<ProductDto>()
                Log.d("ProductRepo", "pullFromServer: ${remoteProducts.size} products from server")

                // 3. Fetch remote variants
                val remoteVariants = postgrest
                    .from("product_variants")
                    .select {
                        filter {
                            eq("business_id", businessId)
                            isNull("deleted_at")
                        }
                    }
                    .decodeList<ProductVariantDto>()

                val variantIds = remoteVariants.map { it.id }

                // 4. Fetch stock and attributes only if there are variants
                val remoteStock = if (variantIds.isNotEmpty()) {
                    postgrest
                        .from("product_stock")
                        .select { filter { isIn("variant_id", variantIds) } }
                        .decodeList<ProductStockDto>()
                } else emptyList()

                val remoteAttributes = if (variantIds.isNotEmpty()) {
                    postgrest
                        .from("variant_attributes_view")
                        .select { filter { isIn("variant_id", variantIds) } }
                        .decodeList<VariantAttributeDto>()
                } else emptyList()

                // 5. Upsert products — skip any that have local unsynced changes
                val productsToUpsert = remoteProducts
                    .filter { it.id !in unsyncedProductIds }
                    .map { it.toProductEntity() }
                if (productsToUpsert.isNotEmpty()) {
                    productDao.insertProducts(productsToUpsert) // REPLACE strategy handles upsert
                }

                // 6. Handle server-side deletes — remove local records that no
                //    longer exist on the server (and aren't pending local deletes)
                val remoteProductIds = remoteProducts.map { it.id }.toSet()
                val localProductIds  = productDao.getAllProductIds(businessId).toSet()
                val toDeleteLocally  = localProductIds
                    .filter { it !in remoteProductIds && it !in unsyncedProductIds }
                toDeleteLocally.forEach { productDao.hardDelete(it) }

                // 7. Upsert variants — skip unsynced
                val variantsToUpsert = remoteVariants
                    .filter { it.id !in unsyncedVariantIds }
                    .map { it.toEntity() }
                if (variantsToUpsert.isNotEmpty()) {
                    variantDao.insertVariants(variantsToUpsert)
                }

                // 8. Handle server-side variant deletes
                val remoteVariantIds = remoteVariants.map { it.id }.toSet()
                val localVariantIds  = variantDao.getAllVariantIdsForBusiness(businessId).toSet()
                val variantsToDelete = localVariantIds
                    .filter { it !in remoteVariantIds && it !in unsyncedVariantIds }
                variantsToDelete.forEach { variantDao.hardDelete(it) }

                // 9. Upsert stock and attributes unconditionally —
                //    these have no local-only state, server is always authoritative
                if (remoteStock.isNotEmpty()) {
                    stockDao.insertStock(remoteStock.map { it.toEntity() })
                }
                if (remoteAttributes.isNotEmpty()) {
                    variantDao.insertAttributes(remoteAttributes.map { it.toEntity() })
                }

                Log.d(
                    "ProductRepo",
                    "pullFromServer: Done — " +
                            "upserted=${productsToUpsert.size}, " +
                            "deleted=${toDeleteLocally.size}, " +
                            "variants=${variantsToUpsert.size}, " +
                            "stock=${remoteStock.size}"
                )

                Resource.Success(Unit)
            } catch (e: Exception) {
                Log.e("ProductRepo", "pullFromServer error: ${e.message}", e)
                Resource.Error(e.message ?: "Pull failed")
            }
        }

    override suspend fun pushPendingProduct(productId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val productWithVariants = productDao.getProductWithVariants(productId)
                    ?: return@withContext Resource.Error("Product not found: $productId")

                val entity = productWithVariants.product

                // Re-upload only content:// URIs (local images not yet uploaded)
                val imagePaths = entity.imageUrls
                    .filter { it.startsWith("content://") }
                    .let { localUris ->
                        if (localUris.isNotEmpty())
                            imageUploadManager.uploadProductImages(
                                imageUris = localUris.map { it.toUri() },
                                businessId = entity.businessId,
                                name = entity.name
                            )
                        else
                            entity.imageUrls.filterNot { it.startsWith("content://") }
                    }

                val request = AddProductRequest(
                    clientId = entity.id,
                    name = entity.name,
                    description = entity.description,
                    category = entity.category,
                    baseSalesPrice = entity.baseSalesPrice,
                    baseCostPrice = entity.baseCostPrice,
                    expiryWarningDays = entity.expiryWarningDays,
                    isActive = entity.isActive,
                    businessId = entity.businessId,
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

                val response = functions.invoke(
                    function = "add_product",
                    body = request
                )
                val syncResponse = response.body<SyncResponse>()

                productDao.insertProduct(
                    entity.copy(
                        imageUrls = imagePaths,
                        updatedAt = syncResponse.updatedAt,
                        syncStatus = Util.SyncStatus.SYNCED
                    )
                )
                productWithVariants.variants.forEach {
                    variantDao.markSynced(it.variant.id, syncResponse.updatedAt)
                }

                // After markSynced calls in pushPendingProduct:
                entity.imageUrls
                    .filter { !it.startsWith("http") }  // only local cached paths
                    .forEach { imageCacheManager.deleteCachedImage(it) }

                Log.d("ProductRepo", "pushPendingProduct: Synced $productId")
                Resource.Success(Unit)
            } catch (e: Exception) {
                Log.e("ProductRepo", "pushPendingProduct failed for $productId: ${e.message}", e)
                Resource.Error(e.message ?: "Sync failed")
            }
        }

    suspend fun pushUpdate(entity: ProductEntity) {
        Log.d("ProductRepo", "pushUpdate: ${entity.id}")
        try {
            postgrest.from("products")
                .update(entity.toProductDto()) {
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
