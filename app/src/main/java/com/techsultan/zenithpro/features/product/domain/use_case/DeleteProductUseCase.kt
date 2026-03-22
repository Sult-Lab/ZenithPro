package com.techsultan.zenithpro.features.product.domain.use_case

import android.net.Uri
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.product.data.remote.AddProductRequest
import com.techsultan.zenithpro.features.product.domain.repository.ProductRepository

class DeleteProductUseCase(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(productId: String): Resource<Unit> =
        repository.deleteProduct(productId)
}