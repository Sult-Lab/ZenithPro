package com.techsultan.zenithpro.features.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.branch.data.local.BranchDao
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.dashboard.data.remote.ChartDataPoint
import com.techsultan.zenithpro.features.dashboard.data.remote.DashboardSummary
import com.techsultan.zenithpro.features.dashboard.data.remote.PendingDebtSummary
import com.techsultan.zenithpro.features.dashboard.domain.use_case.GetChartDataUseCase
import com.techsultan.zenithpro.features.dashboard.domain.use_case.GetDashboardSummaryUseCase
import com.techsultan.zenithpro.features.dashboard.domain.use_case.GetPendingDebtsUseCase
import com.techsultan.zenithpro.features.dashboard.domain.use_case.GetUrgentActionUseCase
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
    private val getUrgentActionUseCase: GetUrgentActionUseCase,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager,
    private val branchDao: BranchDao,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private var businessId: String? = null


    init {
        viewModelScope.launch {
            val session = sessionManager.loadSession() ?: return@launch
            businessId = session.businessId

            _state.update { it.copy(
                isAdmin = session.isAdmin,
                showProfit = session.isAdmin,
                showInventoryAlerts = session.isManager
            ) }

            if (session.isAdmin) {
                val branches = branchDao.getActiveBranchesForBusiness(session.businessId)
                _state.update { it.copy(branches = branches) }
            }

            observeConnectivity()

            // Observe branch changes and reload data
            sessionManager.activeBranchId.collect { id ->
                _state.update { it.copy(
                    activeBranchId = id,
                    activeBranchName = sessionManager.activeBranchName.value,
                )}
                loadAll()
            }
        }
    }


    private fun loadAll() {
        val bId = businessId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val branchId = sessionManager.activeBranchId.value
            
            val summaryJob = async { getDashboardSummaryUseCase(bId, branchId) }
            val chartJob   = async { getChartDataUseCase(bId, _state.value.selectedDays, branchId) }
            val debtJob    = async { getPendingDebtsUseCase(bId, branchId) }
            val urgentJob  = async { getUrgentActionUseCase(bId, branchId) }

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

            val branchId = sessionManager.activeBranchId.value

            syncDashboardUseCase(bId)
            // Reload from Room after sync
            val summary = getDashboardSummaryUseCase(bId, branchId)
            val chart   = getChartDataUseCase(bId, _state.value.selectedDays, branchId)
            val debt    = getPendingDebtsUseCase(bId, branchId)
            val urgent  = getUrgentActionUseCase(bId, branchId)
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
        val branchId = sessionManager.activeBranchId.value

        _state.update { it.copy(selectedDays = days) }
        viewModelScope.launch {
            val chart = getChartDataUseCase(bId, days, branchId)
            _state.update { s ->
                s.copy(chartData = (chart as? Resource.Success)?.data ?: s.chartData)
            }
        }
    }

    fun onBranchSelected(branchId: String?, branchName: String?) {
        if (!_state.value.isAdmin) return // Only admins can switch branches
        viewModelScope.launch {
            sessionManager.setActiveBranch(branchId, branchName)
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
            val branchId = sessionManager.activeBranchId.value

            val result = syncDashboardUseCase(bId)
            if (result is Resource.Success) {
                // Reload Room data after sync completes
                val summary = getDashboardSummaryUseCase(bId, branchId)
                val chart   = getChartDataUseCase(bId, _state.value.selectedDays, branchId)
                val debt    = getPendingDebtsUseCase(bId, branchId)
                val urgent  = getUrgentActionUseCase(bId, branchId)
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
    val error: String? = null,
    val isAdmin: Boolean = false,
    val showProfit: Boolean = false,
    val showInventoryAlerts: Boolean = false,
    val branches: List<BranchEntity> = emptyList(),
    val activeBranchId: String? = null,
    val activeBranchName: String? = null,
)
