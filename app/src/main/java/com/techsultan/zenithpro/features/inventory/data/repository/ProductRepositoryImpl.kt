package com.techsultan.zenithpro.features.inventory.data.repository

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.ImageCacheManager
import com.techsultan.zenithpro.core.util.ImageUploadManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.inventory.data.local.ProductAuditLogDao
import com.techsultan.zenithpro.features.inventory.data.local.ProductAuditLogEntity
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
import com.techsultan.zenithpro.features.inventory.data.remote.ProductAuditLogDto
import com.techsultan.zenithpro.features.inventory.data.remote.ProductDto
import com.techsultan.zenithpro.features.inventory.data.remote.ProductStockDto
import com.techsultan.zenithpro.features.inventory.data.remote.ProductVariantCreateRequest
import com.techsultan.zenithpro.features.inventory.data.remote.ProductVariantDto
import com.techsultan.zenithpro.features.inventory.data.remote.StockCreateRequest
import com.techsultan.zenithpro.features.inventory.data.remote.UpdateProductRequest
import com.techsultan.zenithpro.features.inventory.data.remote.VariantAttributeDto
import com.techsultan.zenithpro.features.inventory.data.remote.VariantAttributeInput
import com.techsultan.zenithpro.features.inventory.domain.repository.ProductRepository
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
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
    private val imageCacheManager: ImageCacheManager,
    private val auditLogDao: ProductAuditLogDao
) : ProductRepository {

    override fun getProducts(businessId: String): Flow<Resource<List<ProductWithVariants>>> =
        productDao.getProductsForBusiness(businessId)
            .map<List<ProductWithVariants>, Resource<List<ProductWithVariants>>> {
                Resource.Success(it)
            }
            .catch { emit(Resource.Error(it.message ?: "Failed to load products")) }
            .onStart { emit(Resource.Loading()) }

    override fun getProduct(productId: String): Flow<Resource<ProductWithVariants>> = flow {
        emit(Resource.Loading())
        try {
            val product = productDao.getProductWithVariants(productId)
            if (product != null) {
                emit(Resource.Success(product))
            } else {
                emit(Resource.Error("Product not found"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load product"))
        }
    }

    override fun getProductsForBranch(
        businessId: String,
        branchId: String
    ): Flow<Resource<List<ProductWithVariants>>> = flow {
        emit(Resource.Loading())
        try {
            productDao.getProductsForBranch(businessId, branchId)
                .collect { products ->
                    emit(Resource.Success(products))
                }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load products"))
        }
    }

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
                    isActive = productRequest.isActive,
                    imageUrls = imageUrls,
                    expiryWarningDays = productRequest.expiryWarningDays,
                    unitType = productRequest.unitType,
                    updatedAt = now,
                    deletedAt = null,
                    syncStatus = Util.SyncStatus.PENDING,
                    locallyCreatedAt = now,
                    branchId = productRequest.branchId
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
                            syncStatus = Util.SyncStatus.PENDING,
                            branchId = productRequest.branchId ?: ""
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

    override suspend fun updateProduct(
        productRequest: AddProductRequest,
        imageUris: List<Uri>
    ): Resource<Unit> {
        Log.d("ProductRepo", "updateProduct: Starting for product: ${productRequest.name}")
        return try {
            val productId = productRequest.clientId
            val now = Instant.now().toString()

            // 1. Handle Images
            val currentProduct = productDao.getProductWithVariants(productId)
            
            // New images (content://) need uploading
            val localUris = imageUris.filter { it.toString().startsWith("content://") }
            val existingRemoteUrls = imageUris.filter { it.toString().startsWith("http") }.map { it.toString() }
            
            val newRemoteUrls = if (localUris.isNotEmpty()) {
                imageUploadManager.uploadProductImages(
                    imageUris = localUris,
                    businessId = productRequest.businessId,
                    name = productRequest.name
                )
            } else emptyList()
            
            val allImageUrls = existingRemoteUrls + newRemoteUrls

            // 2. Update Product locally
            productDao.insertProduct(
                ProductEntity(
                    id = productId,
                    businessId = productRequest.businessId,
                    name = productRequest.name,
                    description = productRequest.description,
                    category = productRequest.category,
                    baseSalesPrice = productRequest.baseSalesPrice,
                    baseCostPrice = productRequest.baseCostPrice,
                    isActive = productRequest.isActive,
                    imageUrls = allImageUrls,
                    expiryWarningDays = productRequest.expiryWarningDays,
                    unitType = productRequest.unitType,
                    updatedAt = now,
                    deletedAt = null,
                    syncStatus = Util.SyncStatus.DIRTY,
                    locallyCreatedAt = currentProduct?.product?.locallyCreatedAt,
                    branchId = productRequest.branchId
                )
            )

            // 3. Update Variants & Stock
            variantDao.deleteVariantsForProduct(productId)
            
            productRequest.variants?.forEach { variantReq ->
                val variantId = variantReq.clientId
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
                        syncStatus = Util.SyncStatus.DIRTY
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
                            syncStatus = Util.SyncStatus.DIRTY,
                            branchId = productRequest.branchId ?: ""
                        )
                    }
                )
            }

            // 4. Sync
            if (networkMonitor.isConnected()) {
                pushNewProduct(productId)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("ProductRepo", "updateProduct error: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to update product")
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
                branchId = productWithVariants.product.branchId,
                imageUrls = productWithVariants.product.imageUrls,
                unitType = productWithVariants.product.unitType,
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
                    Log.d("Product Repo", "Products to Upsert: $productsToUpsert")
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

                // 9. Refresh stock and attributes —
                //    Clear local records for variants we are pulling (excluding unsynced)
                //    to ensure consistency if items were removed on the server.
                val variantIdsToRefresh = variantIds.filter { it !in unsyncedVariantIds }
                if (variantIdsToRefresh.isNotEmpty()) {
                    stockDao.deleteForVariants(variantIdsToRefresh)
                    variantDao.deleteAttributesForVariants(variantIdsToRefresh)
                }

                if (remoteStock.isNotEmpty()) {
                    val stockToInsert = remoteStock
                        .filter { it.variantId in variantIdsToRefresh }
                        .map { it.toEntity() }
                    if (stockToInsert.isNotEmpty()) {
                        stockDao.insertStock(stockToInsert)
                    }
                }
                if (remoteAttributes.isNotEmpty()) {
                    val attrsToInsert = remoteAttributes
                        .filter { it.variantId in variantIdsToRefresh }
                        .map { it.toEntity() }
                    if (attrsToInsert.isNotEmpty()) {
                        variantDao.insertAttributes(attrsToInsert)
                    }
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
                    unitType = entity.unitType,
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

    override suspend fun updateProduct(
        request: UpdateProductRequest,
        newImageUris: List<Uri>
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val now      = Instant.now().toString()
            val existing = productDao.getProductById(request.clientId)
                ?: return@withContext Resource.Error("Product not found")

            // Upload new images and combine with kept existing URLs
            val uploadedUrls: List<String> = if (newImageUris.isNotEmpty()) {
                imageUploadManager.uploadProductImages(
                    imageUris  = newImageUris,
                    businessId = request.businessId,
                    name       = request.name
                )
            } else emptyList()

            val finalImageUrls = request.imageUrls + uploadedUrls

            Log.d("ProductRepo", "updateProduct images: kept=${request.imageUrls.size} new=${uploadedUrls.size} total=${finalImageUrls.size}")

            productDao.updateProduct(
                id                = request.clientId,
                name              = request.name,
                description       = request.description,
                category          = request.category,
                baseSalesPrice    = request.baseSalesPrice,
                baseCostPrice     = request.baseCostPrice,
                expiryWarningDays = request.expiryWarningDays,
                isActive          = request.isActive,
                imageUrls         = finalImageUrls,
                updatedAt         = now,
                syncStatus        = Util.SyncStatus.DIRTY
            )

            reconcileVariantsLocally(
                productId  = request.clientId,
                businessId = request.businessId,
                branchId   = request.branchId,
                variants   = request.variants,
                now        = now
            )

            if (networkMonitor.isConnected()) {
                pushUpdatedProduct(
                    productId = request.clientId,
                    request   = request.copy(imageUrls = finalImageUrls)
                )
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("ProductRepo", "updateProduct: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to update product")
        }
    }

    internal suspend fun pushUpdatedProduct(
        productId: String,
        request: UpdateProductRequest
    ) {
        try {
            val syncRequest = AddProductRequest(
                clientId  = productId,
                name = request.name,
                description = request.description,
                category = request.category,
                baseSalesPrice = request.baseSalesPrice,
                baseCostPrice = request.baseCostPrice,
                expiryWarningDays = request.expiryWarningDays,
                unitType = request.unitType,
                isActive = request.isActive,
                businessId = request.businessId,
                imageUrls = request.imageUrls,
                variants = request.variants
            )

            val response = functions.invoke(
                function = "add_product",
                body     = syncRequest
            )

            val syncResponse = response.body<SyncResponse>()
            productDao.markSynced(productId, syncResponse.updatedAt)

            // Mark all variants synced too
            request.variants.forEach {
                variantDao.markSynced(it.clientId, syncResponse.updatedAt)
            }

            Log.d("ProductRepo", "pushUpdatedProduct: synced $productId")
        } catch (e: Exception) {
            Log.w("ProductRepo", "pushUpdatedProduct failed for $productId: ${e.message}")
            // Left as DIRTY — SyncManager will retry
        }
    }

    private suspend fun reconcileVariantsLocally(
        productId: String,
        businessId: String,
        branchId: String?,
        variants: List<ProductVariantCreateRequest>,
        now: String
    ) {
        val existingVariants = variantDao.getVariantsForProduct(productId)
        val existingIds      = existingVariants.map { it.id }.toSet()
        val incomingIds      = variants.map { it.clientId }.toSet()

        // Soft-delete variants removed during edit
        // Soft-delete preserves FK reference from sale_items
        existingIds
            .filter { it !in incomingIds }
            .forEach { removedId ->
                variantDao.softDelete(removedId, now)
                // Also clean up stock and attributes for removed variants
                stockDao.deleteForVariant(removedId)
                variantDao.deleteAttributesForVariant(removedId)
            }

        // Upsert each incoming variant
        variants.forEach { variantReq ->
            if (variantReq.clientId in existingIds) {
                // Update existing — never touch SKU
                variantDao.updateVariant(
                    id         = variantReq.clientId,
                    sku        = existingVariants.first { it.id == variantReq.clientId }.sku,
                    salesPrice = variantReq.salesPrice,
                    costPrice  = variantReq.costPrice,
                    barcode    = variantReq.barcode,
                    updatedAt  = now,
                    syncStatus = Util.SyncStatus.DIRTY
                )
            } else {
                // New variant added during edit
                variantDao.insertVariant(
                    ProductVariantEntity(
                        id         = variantReq.clientId,
                        productId  = productId,
                        businessId = businessId,
                        sku        = variantReq.sku,
                        salesPrice = variantReq.salesPrice,
                        costPrice  = variantReq.costPrice,
                        barcode    = variantReq.barcode,
                        updatedAt  = now,
                        deletedAt  = null,
                        syncStatus = Util.SyncStatus.PENDING
                    )
                )
            }

            // Replace attributes entirely — delete then reinsert
            variantDao.deleteAttributesForVariant(variantReq.clientId)
            if (variantReq.attributes.isNotEmpty()) {
                variantDao.insertAttributes(
                    variantReq.attributes.map {
                        VariantAttributeEntity(
                            id          = UUID.randomUUID().toString(),
                            variantId   = variantReq.clientId,
                            optionName  = it.optionName,
                            optionValue = it.optionValue
                        )
                    }
                )
            }

            // Replace stock — delete then reinsert
            stockDao.deleteForVariant(variantReq.clientId)
            stockDao.insertStock(
                variantReq.stock.map {
                    ProductStockEntity(
                        id            = UUID.randomUUID().toString(),
                        variantId     = variantReq.clientId,
                        quantity      = it.quantity,
                        expiryDate    = it.expiryDate,
                        lowStockAlert = it.lowStockAlert,
                        updatedAt     = now,
                        syncStatus    = Util.SyncStatus.DIRTY,
                        branchId      = branchId ?: ""
                    )
                }
            )
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

    override fun getProductAuditLogs(productId: String): Flow<Resource<List<ProductAuditLogEntity>>> = flow {
        emit(Resource.Loading())

        // 1. Emit local cache first
        auditLogDao.observeLogsForProduct(productId).collect { localLogs ->
            emit(Resource.Success(localLogs))

            // 2. Then try to pull from server if connected
            if (networkMonitor.isConnected()) {
                try {
                    val remoteLogs = postgrest
                        .from("product_audit_log")
                        .select {
                            filter { eq("product_id", productId) }
                            order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                            limit(50)
                        }
                        .decodeList<ProductAuditLogDto>()

                    auditLogDao.insertAll(remoteLogs.map { it.toEntity() })
                } catch (e: Exception) {
                    Log.e("ProductRepo", "Failed to pull audit logs: ${e.message}")
                    // We don't emit error here because we already emitted local data
                }
            }
        }
    }.catch { emit(Resource.Error(it.message ?: "Failed to load audit logs")) }

}

@Serializable
data class SyncResponse(
    @SerialName("productId") val productId: String,
    @SerialName("updatedAt") val updatedAt: String
)
