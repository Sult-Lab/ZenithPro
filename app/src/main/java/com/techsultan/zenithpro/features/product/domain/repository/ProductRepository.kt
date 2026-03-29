package com.techsultan.zenithpro.features.product.domain.repository

import android.net.Uri
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.product.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.product.data.remote.AddProductRequest
import com.techsultan.zenithpro.features.product.data.remote.ProductVariantCreate
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(businessId: String): Flow<Resource<List<ProductWithVariants>>>
    suspend fun addProduct(productRequest: AddProductRequest, imageUris: List<Uri>): Resource<Unit>
    suspend fun deleteProduct(productId: String): Resource<Unit>
    suspend fun pullFromServer(businessId: String): Resource<Unit>
    suspend fun pushPendingProduct(productId: String): Resource<Unit>
}
