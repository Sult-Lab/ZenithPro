package com.techsultan.zenithpro.features.production.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.production.data.local.ProductionOrderEntity
import com.techsultan.zenithpro.features.production.domain.repository.ProductionRepository

class CompleteProductionOrderUseCase(private val repository: ProductionRepository) {
    suspend operator fun invoke(
        orderId: String, businessId: String, staffId: String
    ) = repository.completeOrder(orderId, businessId, staffId)
}