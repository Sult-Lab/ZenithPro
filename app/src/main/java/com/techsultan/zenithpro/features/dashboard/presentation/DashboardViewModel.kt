package com.techsultan.zenithpro.features.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.dashboard.data.remote.ChartDataPoint
import com.techsultan.zenithpro.features.dashboard.data.remote.DashboardSummary
import com.techsultan.zenithpro.features.dashboard.data.remote.PendingDebtSummary
import com.techsultan.zenithpro.features.dashboard.domain.use_case.GetChartDataUseCase
import com.techsultan.zenithpro.features.dashboard.domain.use_case.GetDashboardSummaryUseCase
import com.techsultan.zenithpro.features.dashboard.domain.use_case.GetPendingDebtsUseCase
import com.techsultan.zenithpro.features.dashboard.domain.use_case.GetUrgentActionsUseCase
import com.techsultan.zenithpro.features.dashboard.domain.use_case.SyncDashboardUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    private val getChartDataUseCase: GetChartDataUseCase,
    private val getPendingDebtsUseCase: GetPendingDebtsUseCase,
    private val syncDashboardUseCase: SyncDashboardUseCase,
    private val getUrgentActionsUseCase: GetUrgentActionsUseCase,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private var businessId: String? = null


    init {
        viewModelScope.launch {
            businessId = sessionManager.loadSession()?.businessId
            if (businessId != null) {
                loadAll()
                observeConnectivity()
            }
        }
    }


    private fun loadAll() {
        val bId = businessId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // All three load from local Room — fast, no network needed
            val summaryJob = async { getDashboardSummaryUseCase(bId) }
            val chartJob   = async { getChartDataUseCase(bId, 7) }
            val debtJob    = async { getPendingDebtsUseCase(bId) }
            val urgentJob  = async { getUrgentActionsUseCase(bId) }

            val summary = summaryJob.await()
            val chart   = chartJob.await()
            val debt    = debtJob.await()
            val urgent  = urgentJob.await()

            _state.update { s ->
                s.copy(
                    isLoading = false,
                    summary = (summary as? Resource.Success)?.data ?: s.summary,
                    chartData = (chart   as? Resource.Success)?.data ?: s.chartData,
                    pendingDebts  = (debt    as? Resource.Success)?.data ?: s.pendingDebts,
                    lowStockCount = urgent.first,
                    pendingPurchaseOrderCount = urgent.second,
                    error = (summary as? Resource.Error)?.message,
                )
            }

            // Sync in background after local data is shown
            syncInBackground()
        }
    }

    // ── Manual pull-to-refresh ──────────────────────────────────────

    fun refresh() {
        val bId = businessId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            
            syncDashboardUseCase(bId)
            // Reload from Room after sync
            val summary = getDashboardSummaryUseCase(bId)
            val chart   = getChartDataUseCase(bId, _state.value.selectedDays)
            val debt    = getPendingDebtsUseCase(bId)
            val urgent  = getUrgentActionsUseCase(bId)
            _state.update { s ->
                s.copy(
                    isRefreshing = false,
                    summary = (summary as? Resource.Success)?.data ?: s.summary,
                    chartData = (chart   as? Resource.Success)?.data ?: s.chartData,
                    pendingDebts = (debt    as? Resource.Success)?.data ?: s.pendingDebts,
                    lowStockCount = urgent.first,
                    pendingPurchaseOrderCount = urgent.second
                )
            }
        }
    }

    fun onChartPeriodChanged(days: Int) {
        val bId = businessId ?: return
        _state.update { it.copy(selectedDays = days) }
        viewModelScope.launch {
            val chart = getChartDataUseCase(bId, days)
            _state.update { s ->
                s.copy(chartData = (chart as? Resource.Success)?.data ?: s.chartData)
            }
        }
    }


    private fun observeConnectivity() {
        viewModelScope.launch {
            networkMonitor.isConnectedFlow
                .filter { it }
                .collect { syncInBackground() }
        }
    }

    private fun syncInBackground() {
        val bId = businessId ?: return
        viewModelScope.launch {
            val result = syncDashboardUseCase(bId)
            if (result is Resource.Success) {
                // Reload Room data after sync completes
                val summary = getDashboardSummaryUseCase(bId)
                val chart   = getChartDataUseCase(bId, _state.value.selectedDays)
                val debt    = getPendingDebtsUseCase(bId)
                val urgent  = getUrgentActionsUseCase(bId)
                _state.update { s ->
                    s.copy(
                        summary = (summary as? Resource.Success)?.data ?: s.summary,
                        chartData = (chart   as? Resource.Success)?.data ?: s.chartData,
                        pendingDebts = (debt    as? Resource.Success)?.data ?: s.pendingDebts,
                        lowStockCount = urgent.first,
                        pendingPurchaseOrderCount = urgent.second
                    )
                }
            }
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean  = false,
    val isRefreshing: Boolean  = false,
    val summary: DashboardSummary = DashboardSummary(),
    val chartData: List<ChartDataPoint> = emptyList(),
    val pendingDebts: PendingDebtSummary = PendingDebtSummary(),
    val selectedDays: Int  = 7,
    val lowStockCount: Int  = 0,
    val pendingPurchaseOrderCount: Int  = 0,
    val error: String? = null
)
