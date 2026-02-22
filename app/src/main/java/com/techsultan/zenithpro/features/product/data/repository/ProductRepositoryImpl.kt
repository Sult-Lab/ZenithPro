package com.techsultan.zenithpro.features.product.data.repository

import android.net.Uri
import android.util.Log
import com.techsultan.zenithpro.core.util.ImageUploadManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.product.data.remote.AddProductRequest
import com.techsultan.zenithpro.features.product.data.remote.CreateVariantsRequest
import com.techsultan.zenithpro.features.product.data.remote.ProductVariantCreate
import com.techsultan.zenithpro.features.product.data.remote.ProductVariantCreateRequest
import com.techsultan.zenithpro.features.product.data.remote.StockCreateRequest
import com.techsultan.zenithpro.features.product.domain.repository.ProductRepository
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest

class ProductRepositoryImpl(
    private val imageUploadManager: ImageUploadManager,
    private val postgrest: Postgrest,
    private val functions: Functions,
) : ProductRepository {

    override suspend fun addProduct(
        product: AddProductRequest,
        imageUris: List<Uri>,
    ): Resource<Unit> {
        return try {

            val imagePaths: List<String> = if (imageUris.isNotEmpty()) {
                imageUploadManager.uploadProductImages(imageUris, product.businessId)
            } else {
                emptyList()
            }
            Log.d("Add product Repo", "Image paths: $imagePaths")
            val request = AddProductRequest(
                name = product.name,
                description = product.description,
                category = product.category,
                baseSalesPrice = product.baseSalesPrice,
                baseCostPrice = product.baseCostPrice,
                isActive = product.isActive,
                businessId = product.businessId,
                imageUrls = imagePaths,
                variants = product.variants?.map { variant ->
                    ProductVariantCreateRequest(
                        sku = variant.sku,
                        salesPrice = variant.salesPrice,
                        costPrice = variant.costPrice,
                        barcode = variant.barcode,
                        attributes = variant.attributes,
                        stock = variant.stock.map {
                            StockCreateRequest(
                                quantity = it.quantity,
                                expiryDate = it.expiryDate,
                                variantId = it.variantId,
                                lowStockAlert = it.lowStockAlert
                            )
                        }
                    )
                } ?: emptyList()
            )


            functions.invoke(
                function = "add_product",
                body = request
            )

            Resource.Success(Unit)

        } catch (e: Exception) {
            Log.e("AddProduct", "Error: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to add product")
        }
    }

    override suspend fun createVariants(
        productId: String,
        productVariants: List<ProductVariantCreate>
    ): Resource<Unit> {
        return try {
            functions.invoke(
                function = "create_variants",
                body = CreateVariantsRequest(
                    productId = productId,
                    variants = productVariants
                )
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create variants")
        }
    }
}
