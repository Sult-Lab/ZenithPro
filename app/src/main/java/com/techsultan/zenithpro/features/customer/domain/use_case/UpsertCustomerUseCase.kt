package com.techsultan.zenithpro.features.customer.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.data.remote.CustomerRequest
import com.techsultan.zenithpro.features.customer.domain.repository.CustomerRepository

class UpsertCustomerUseCase(private val repository: CustomerRepository) {
    suspend operator fun invoke(
        request: CustomerRequest,
        businessId: String
    ): Resource<CustomerEntity> {
        if (request.firstName.isBlank()) {
            return Resource.Error("First name is required")
        }
        return repository.upsertCustomer(request, businessId)
    }
}