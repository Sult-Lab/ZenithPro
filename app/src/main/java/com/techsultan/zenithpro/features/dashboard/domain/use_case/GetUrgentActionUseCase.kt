package com.techsultan.zenithpro.features.dashboard.domain.use_case

import com.techsultan.zenithpro.features.inventory.data.local.ProductDao

class GetUrgentActionUseCase(
    private val productDao: ProductDao,
) {
    suspend operator fun invoke(businessId: String, branchId: String? = null): Pair<Int, Int> {
        val lowStock      = productDao.getLowStockCount(businessId, branchId)
        val pendingOrders = productDao.getPendingPurchaseOrderCount()
        return lowStock to pendingOrders
    }
}
