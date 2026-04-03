package com.techsultan.zenithpro.features.customer.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.data.remote.CustomerRequest
import com.techsultan.zenithpro.features.customer.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow

class GetCustomersUseCase(private val repository: CustomerRepository) {
    operator fun invoke(
        businessId: String,
        query: String = ""
    ): Flow<Resource<List<CustomerEntity>>> =
        if (query.isBlank()) repository.getCustomers(businessId)
        else repository.searchCustomers(businessId, query)
}