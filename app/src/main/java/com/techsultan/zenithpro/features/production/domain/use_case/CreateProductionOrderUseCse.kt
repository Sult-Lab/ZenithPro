package com.techsultan.zenithpro.features.production.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.production.data.local.ProductionOrderEntity
import com.techsultan.zenithpro.features.production.domain.repository.ProductionRepository

class CreateProductionOrderUseCase(private val repository: ProductionRepository) {
    suspend operator fun invoke(
        variantId: String, quantity: Double, branchId: String?,
        notes: String?, businessId: String, staffId: String
    ): Resource<ProductionOrderEntity> {
        if (quantity <= 0) return Resource.Error("Quantity must be greater than zero")
        return repository.createOrder(variantId, quantity, branchId, notes, businessId, staffId)
    }
}