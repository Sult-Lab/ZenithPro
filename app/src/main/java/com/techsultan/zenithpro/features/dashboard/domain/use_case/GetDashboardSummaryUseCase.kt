package com.techsultan.zenithpro.features.dashboard.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.dashboard.data.remote.DashboardSummary
import com.techsultan.zenithpro.features.dashboard.domain.repository.DashboardRepository

class GetDashboardSummaryUseCase(private val repository: DashboardRepository) {
    suspend operator fun invoke(
        businessId: String,
        branchId: String? = null
    ): Resource<DashboardSummary> =
        repository.getTodaySummary(businessId, branchId)
}