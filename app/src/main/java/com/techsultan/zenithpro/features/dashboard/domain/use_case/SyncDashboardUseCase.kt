package com.techsultan.zenithpro.features.dashboard.domain.use_case

import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.dashboard.data.remote.ChartDataPoint
import com.techsultan.zenithpro.features.dashboard.data.remote.PendingDebtSummary
import com.techsultan.zenithpro.features.dashboard.domain.repository.DashboardRepository

class SyncDashboardUseCase(
    private val repository: DashboardRepository,
    private val networkMonitor: NetworkMonitor,
) {
    suspend operator fun invoke(businessId: String): Resource<Unit> {
        if (!networkMonitor.isConnected()) return Resource.Success(Unit) // silently skip
        return repository.syncDashboard(businessId)
    }
}