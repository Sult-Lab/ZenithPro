package com.techsultan.zenithpro.features.production.domain.repository

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.production.data.local.ProductionOrderEntity
import kotlinx.coroutines.flow.Flow

interface ProductionRepository {
    fun getOrders(businessId: String, status: String? = null): Flow<Resource<List<ProductionOrderEntity>>>
    suspend fun createOrder(
        variantId: String, quantity: Double, branchId: String?,
        notes: String?, businessId: String, staffId: String
    ): Resource<ProductionOrderEntity>
    suspend fun startOrder(orderId: String): Resource<Unit>
    suspend fun completeOrder(orderId: String, businessId: String, staffId: String): Resource<Unit>
    suspend fun cancelOrder(orderId: String): Resource<Unit>
    suspend fun getPendingOrderCount(businessId: String): Int
    suspend fun pullFromServer(businessId: String): Resource<Unit>
}