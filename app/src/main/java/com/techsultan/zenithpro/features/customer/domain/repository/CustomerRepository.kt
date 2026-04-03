package com.techsultan.zenithpro.features.customer.domain.repository

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.data.remote.CustomerRequest
import com.techsultan.zenithpro.features.customer.data.remote.CustomerStats
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun getCustomers(businessId: String): Flow<Resource<List<CustomerEntity>>>
    fun searchCustomers(businessId: String, query: String): Flow<Resource<List<CustomerEntity>>>
    fun observeCustomer(customerId: String): Flow<Resource<CustomerEntity?>>
    fun getCustomersWithDebt(businessId: String): Flow<Resource<List<CustomerEntity>>>
    suspend fun upsertCustomer(request: CustomerRequest, businessId: String): Resource<CustomerEntity>
    suspend fun deleteCustomer(customerId: String): Resource<Unit>
    suspend fun getTopSpenders(businessId: String, limit: Int = 10): Resource<List<CustomerEntity>>
    suspend fun getMostLoyal(businessId: String, limit: Int = 10): Resource<List<CustomerEntity>>
    suspend fun getCustomerStats(businessId: String): Resource<CustomerStats>
    suspend fun getCustomerSales(customerId: String, businessId: String): Resource<List<SaleWithItems>>
    suspend fun pullFromServer(businessId: String): Resource<Unit>
}