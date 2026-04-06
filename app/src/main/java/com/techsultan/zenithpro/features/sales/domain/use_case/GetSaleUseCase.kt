package com.techsultan.zenithpro.features.sales.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import com.techsultan.zenithpro.features.sales.data.remote.SaleFilter
import com.techsultan.zenithpro.features.sales.domain.repository.SaleRepository
import kotlinx.coroutines.flow.Flow

class GetSalesUseCase(private val repository: SaleRepository) {
    operator fun invoke(businessId: String, filter: SaleFilter? = null): Flow<Resource<List<SaleWithItems>>> =
        if (filter != null) repository.getSalesFiltered(businessId, filter)
        else repository.getSales(businessId)
}