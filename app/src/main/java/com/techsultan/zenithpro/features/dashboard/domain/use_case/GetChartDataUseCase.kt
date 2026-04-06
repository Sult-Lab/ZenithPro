package com.techsultan.zenithpro.features.dashboard.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.dashboard.data.remote.ChartDataPoint
import com.techsultan.zenithpro.features.dashboard.domain.repository.DashboardRepository

class GetChartDataUseCase(private val repository: DashboardRepository) {
    suspend operator fun invoke(
        businessId: String,
        days: Int = 7
    ): Resource<List<ChartDataPoint>> = repository.getChartData(businessId, days)
}