package com.techsultan.zenithpro.features.sales.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import com.techsultan.zenithpro.features.sales.data.remote.DailySummary
import com.techsultan.zenithpro.features.sales.data.remote.SaleFilter
import com.techsultan.zenithpro.features.sales.domain.repository.SaleRepository
import kotlinx.coroutines.flow.Flow

class GetDailySummaryUseCase(private val repository: SaleRepository) {
    suspend operator fun invoke(businessId: String): Resource<DailySummary> =
        repository.getDailySummary(businessId)
}