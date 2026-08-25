package com.techsultan.zenithpro.features.dashboard.domain.repository

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.dashboard.data.remote.ChartDataPoint
import com.techsultan.zenithpro.features.dashboard.data.remote.DashboardSummary
import com.techsultan.zenithpro.features.dashboard.data.remote.PendingDebtSummary

interface DashboardRepository {
    suspend fun getTodaySummary(businessId: String, branchId: String? = null): Resource<DashboardSummary>
    suspend fun getChartData(businessId: String, days: Int = 7, branchId: String? = null): Resource<List<ChartDataPoint>>
    suspend fun getPendingDebts(businessId: String, branchId: String? = null): Resource<PendingDebtSummary>
    suspend fun syncDashboard(businessId: String): Resource<Unit>
}