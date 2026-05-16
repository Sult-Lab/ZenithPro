package com.techsultan.zenithpro.features.inventory.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.inventory.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow

class GetProductUseCase(
    private val repository: ProductRepository
) {
    operator fun invoke(productId: String): Flow<Resource<ProductWithVariants>> =
        repository.getProduct(productId)
}
