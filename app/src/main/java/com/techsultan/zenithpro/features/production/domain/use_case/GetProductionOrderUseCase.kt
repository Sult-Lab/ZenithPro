package com.techsultan.zenithpro.features.production.domain.use_case

import com.techsultan.zenithpro.features.production.domain.repository.ProductionRepository

class GetProductionOrdersUseCase(private val repository: ProductionRepository) {
    operator fun invoke(businessId: String, status: String? = null) =
        repository.getOrders(businessId, status)
}