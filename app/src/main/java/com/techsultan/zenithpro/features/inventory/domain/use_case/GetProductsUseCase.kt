package com.techsultan.zenithpro.features.inventory.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.inventory.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow

class GetProductsUseCase(
    private val productRepository: ProductRepository
) {
    // All branches — Admin with no filter
    operator fun invoke(businessId: String): Flow<Resource<List<ProductWithVariants>>> {
        return productRepository.getProducts(businessId)
    }

    // Branch scoped — Staff, Manager, or Admin with branch selected
    operator fun invoke(
        businessId: String,
        branchId: String
    ): Flow<Resource<List<ProductWithVariants>>> {
        return productRepository.getProductsForBranch(businessId, branchId)
    }
}