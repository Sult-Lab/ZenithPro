package com.techsultan.zenithpro.features.product.domain.repository

import android.net.Uri
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.product.data.remote.AddProductRequest
import com.techsultan.zenithpro.features.product.data.remote.ProductVariantCreate

interface ProductRepository {
   // suspend fun createProductImageBucket(businessName: String)
    suspend fun addProduct(product: AddProductRequest, imageUris: List<Uri>): Resource<Unit>
    suspend fun createVariants(productId: String, productVariants: List<ProductVariantCreate>): Resource<Unit>
}