package com.techsultan.zenithpro.features.customer.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.data.remote.CustomerRequest
import com.techsultan.zenithpro.features.customer.data.remote.CustomerStats
import com.techsultan.zenithpro.features.customer.domain.repository.CustomerRepository
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import kotlinx.coroutines.flow.Flow

class GetCustomerReportsUseCase(private val repository: CustomerRepository) {
    suspend operator fun invoke(businessId: String): Resource<CustomerReportData> {
        return try {
            val spenders = repository.getTopSpenders(businessId)
            val loyal    = repository.getMostLoyal(businessId)
            val stats    = repository.getCustomerStats(businessId)

            Resource.Success(
                CustomerReportData(
                    topSpenders    = (spenders as? Resource.Success)?.data ?: emptyList(),
                    mostLoyal      = (loyal    as? Resource.Success)?.data ?: emptyList(),
                    stats          = (stats    as? Resource.Success)?.data ?: CustomerStats()
                )
            )
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load report")
        }
    }
}

data class CustomerReportData(
    val topSpenders: List<CustomerEntity>,
    val mostLoyal: List<CustomerEntity>,
    val stats: CustomerStats
)