package com.techsultan.zenithpro.features.inventory.domain.repository

import android.net.Uri
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.inventory.data.remote.AddProductRequest
import com.techsultan.zenithpro.features.inventory.data.remote.UpdateProductRequest
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(businessId: String): Flow<Resource<List<ProductWithVariants>>>
    fun getProduct(productId: String): Flow<Resource<ProductWithVariants>>
    suspend fun addProduct(productRequest: AddProductRequest, imageUris: List<Uri>): Resource<Unit>
    suspend fun updateProduct(productRequest: AddProductRequest, imageUris: List<Uri>): Resource<Unit>
    suspend fun deleteProduct(productId: String): Resource<Unit>
    suspend fun pullFromServer(businessId: String): Resource<Unit>
    suspend fun pushPendingProduct(productId: String): Resource<Unit>

    suspend fun updateProduct(
        request: UpdateProductRequest,
        newImageUris: List<Uri>
    ): Resource<Unit>
}
