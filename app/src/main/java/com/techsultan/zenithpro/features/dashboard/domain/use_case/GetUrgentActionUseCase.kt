package com.techsultan.zenithpro.features.dashboard.domain.use_case

import com.techsultan.zenithpro.features.product.data.local.ProductDao

class GetUrgentActionsUseCase(
    private val productDao: ProductDao,
) {
    suspend operator fun invoke(businessId: String): Pair<Int, Int> {
        val lowStock      = productDao.getLowStockCount(businessId)
        val pendingOrders = productDao.getPendingPurchaseOrderCount()
        return lowStock to pendingOrders
    }
}