package com.techsultan.zenithpro.features.inventory.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.inventory.domain.repository.ProductRepository

class DeleteProductUseCase(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(productId: String): Resource<Unit> =
        repository.deleteProduct(productId)
}