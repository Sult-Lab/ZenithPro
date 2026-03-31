package com.techsultan.zenithpro.features.sales.domain.repository

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import com.techsultan.zenithpro.features.sales.data.remote.CartItem
import com.techsultan.zenithpro.features.sales.data.remote.DailySummary
import com.techsultan.zenithpro.features.sales.data.remote.DebtPaymentRequest
import com.techsultan.zenithpro.features.sales.data.remote.ProcessSaleRequest
import com.techsultan.zenithpro.features.sales.data.remote.ProcessSaleResponse
import com.techsultan.zenithpro.features.sales.data.remote.SaleFilter
import kotlinx.coroutines.flow.Flow

interface SaleRepository {
    fun getSales(businessId: String): Flow<Resource<List<SaleWithItems>>>
    fun getSalesFiltered(businessId: String, filter: SaleFilter): Flow<Resource<List<SaleWithItems>>>
    suspend fun processSale(request: ProcessSaleRequest, cart: List<CartItem>): Resource<ProcessSaleResponse>
    suspend fun recordDebtPayment(request: DebtPaymentRequest): Resource<Unit>
    suspend fun pullSalesFromServer(businessId: String): Resource<Unit>
    suspend fun getDailySummary(businessId: String): Resource<DailySummary>
}