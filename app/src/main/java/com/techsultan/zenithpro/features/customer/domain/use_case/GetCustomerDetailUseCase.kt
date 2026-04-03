package com.techsultan.zenithpro.features.customer.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.data.remote.CustomerRequest
import com.techsultan.zenithpro.features.customer.domain.repository.CustomerRepository
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import kotlinx.coroutines.flow.Flow

class GetCustomerDetailUseCase(
    private val repository: CustomerRepository,
) {
    operator fun invoke(customerId: String): Flow<Resource<CustomerEntity?>> =
        repository.observeCustomer(customerId)

    suspend fun getSales(
        customerId: String,
        businessId: String
    ): Resource<List<SaleWithItems>> =
        repository.getCustomerSales(customerId, businessId)
}