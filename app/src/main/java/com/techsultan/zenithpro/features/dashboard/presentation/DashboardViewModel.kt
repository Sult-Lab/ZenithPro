package com.techsultan.zenithpro.features.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.dashboard.data.remote.ChartDataPoint
import com.techsultan.zenithpro.features.dashboard.data.remote.DashboardSummary
import com.techsultan.zenithpro.features.dashboard.data.remote.PendingDebtSummary
import com.techsultan.zenithpro.features.dashboard.domain.use_case.GetChartDataUseCase
import com.techsultan.zenithpro.features.dashboard.domain.use_case.GetDashboardSummaryUseCase
import com.techsultan.zenithpro.features.dashboard.domain.use_case.GetPendingDebtsUseCase
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
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private var businessId = ""

    fun init(businessId: String) {
        if (this.businessId == businessId) return
        this.businessId = businessId
        loadAll()
        observeConnectivity()
    }

    // ── Load from Room immediately, then sync in background ────────

    private fun loadAll() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // All three load from local Room — fast, no network needed
            val summaryJob = async { getDashboardSummaryUseCase(businessId) }
            val chartJob   = async { getChartDataUseCase(businessId, 7) }
            val debtJob    = async { getPendingDebtsUseCase(businessId) }

            val summary = summaryJob.await()
            val chart   = chartJob.await()
            val debt    = debtJob.await()

            _state.update { s ->
                s.copy(
                    isLoading     = false,
                    summary       = (summary as? Resource.Success)?.data ?: s.summary,
                    chartData     = (chart   as? Resource.Success)?.data ?: s.chartData,
                    pendingDebts  = (debt    as? Resource.Success)?.data ?: s.pendingDebts,
                    error         = (summary as? Resource.Error)?.message
                )
            }

            // Sync in background after local data is shown
            syncInBackground()
        }
    }

    // ── Manual pull-to-refresh ──────────────────────────────────────

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            syncDashboardUseCase(businessId)
            // Reload from Room after sync
            val summary = getDashboardSummaryUseCase(businessId)
            val chart   = getChartDataUseCase(businessId, _state.value.selectedDays)
            val debt    = getPendingDebtsUseCase(businessId)
            _state.update { s ->
                s.copy(
                    isRefreshing = false,
                    summary      = (summary as? Resource.Success)?.data ?: s.summary,
                    chartData    = (chart   as? Resource.Success)?.data ?: s.chartData,
                    pendingDebts = (debt    as? Resource.Success)?.data ?: s.pendingDebts
                )
            }
        }
    }

    // ── Chart period selector ───────────────────────────────────────

    fun onChartPeriodChanged(days: Int) {
        _state.update { it.copy(selectedDays = days) }
        viewModelScope.launch {
            val chart = getChartDataUseCase(businessId, days)
            _state.update { s ->
                s.copy(chartData = (chart as? Resource.Success)?.data ?: s.chartData)
            }
        }
    }

    // ── Auto-sync on connectivity restore ──────────────────────────

    private fun observeConnectivity() {
        viewModelScope.launch {
            networkMonitor.isConnectedFlow
                .filter { it }
                .collect { syncInBackground() }
        }
    }

    private fun syncInBackground() {
        viewModelScope.launch {
            val result = syncDashboardUseCase(businessId)
            if (result is Resource.Success) {
                // Reload Room data after sync completes
                val summary = getDashboardSummaryUseCase(businessId)
                val chart   = getChartDataUseCase(businessId, _state.value.selectedDays)
                val debt    = getPendingDebtsUseCase(businessId)
                _state.update { s ->
                    s.copy(
                        summary      = (summary as? Resource.Success)?.data ?: s.summary,
                        chartData    = (chart   as? Resource.Success)?.data ?: s.chartData,
                        pendingDebts = (debt    as? Resource.Success)?.data ?: s.pendingDebts
                    )
                }
            }
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean          = false,
    val isRefreshing: Boolean       = false,
    val summary: DashboardSummary = DashboardSummary(),
    val chartData: List<ChartDataPoint> = emptyList(),
    val pendingDebts: PendingDebtSummary = PendingDebtSummary(),
    val selectedDays: Int           = 7,
    val error: String?              = null
)