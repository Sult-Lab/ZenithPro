package com.techsultan.zenithpro.features.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.analytics.data.ReportPeriod
import com.techsultan.zenithpro.features.analytics.data.remote.ReportsData
import com.techsultan.zenithpro.features.analytics.domain.ReportsRepository
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.branch.domain.repository.BranchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReportsViewModel(
    private val reportsRepository: ReportsRepository,
    private val branchRepository: BranchRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    private var businessId: String? = null

    init {
        viewModelScope.launch { 
            sessionManager.loadSession()?.let { 
                init(it.businessId)
            }
        }
    }

    fun init(businessId: String) {
        this.businessId = businessId
        val session = sessionManager.currentSession

        if (session?.isStaff == true) {
            _state.update {
                it.copy(
                    error = "You don't have permission to view reports",
                    isLoading = false
                )
            }
            return
        }
        // Managers are locked to their own branch if assigned
        if (session?.isAdmin == false) {
            _state.update {
                it.copy(
                    filterBranchId = session.branchId,
                    canFilterBranch = session.branchId == null,
                    canFilterStaff  = true, // Can see staff in their branch(es)
                    showProfit = false,
                    canExport = false
                )
            }
        } else {
            _state.update {
                it.copy(
                    canFilterBranch = true,
                    canFilterStaff = true,
                    showProfit = true,
                    canExport = true
                )
            }
        }

        loadBranches()
        loadReport()
    }

    private fun loadBranches() {
        val bId = businessId ?: return
        viewModelScope.launch {
            branchRepository.getBranches(bId).collect { result ->
                if (result is Resource.Success) {
                    _state.update { it.copy(branches = result.data?.filter { b -> b.isActive } ?: emptyList()) }
                }
            }
        }
    }

    fun onPeriodChanged(period: ReportPeriod) {
        _state.update { it.copy(selectedPeriod = period) }
        loadReport()
    }

    fun onCustomDateRangeChanged(from: LocalDate, to: LocalDate) {
        _state.update {
            it.copy(
                selectedPeriod  = ReportPeriod.CUSTOM,
                customFrom      = from,
                customTo        = to
            )
        }
        loadReport()
    }

    fun onBranchFilterChanged(branchId: String?) {
        if (!_state.value.canFilterBranch) return
        _state.update { it.copy(filterBranchId = branchId) }
        loadReport()
    }

    fun onStaffFilterChanged(staffId: String?) {
        if (!_state.value.canFilterStaff) return
        _state.update { it.copy(filterStaffId = staffId) }
        loadReport()
    }

    fun clearFilters() {
        val session = sessionManager.currentSession
        _state.update {
            it.copy(
                filterBranchId = if (it.canFilterBranch) null else session?.branchId,
                filterStaffId  = if (it.canFilterStaff) null else session?.userId
            )
        }
        loadReport()
    }

    fun refresh() {
        viewModelScope.launch {
            val bId = businessId ?: return@launch
            _state.update { it.copy(isRefreshing = true) }
            reportsRepository.syncAll(bId)
            loadReportSuspend()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadReport() {
        viewModelScope.launch { loadReportSuspend() }
    }

    private suspend fun loadReportSuspend() {
        val bId = businessId ?: return
        val s = _state.value
        _state.update { it.copy(isLoading = true) }

        val result = reportsRepository.getReportData(
            businessId = bId,
            period     = s.selectedPeriod,
            branchId   = s.filterBranchId,
            staffId    = s.filterStaffId
        )

        when (result) {
            is Resource.Success -> _state.update {
                it.copy(isLoading = false, data = result.data, error = null)
            }
            is Resource.Error -> _state.update {
                it.copy(isLoading = false, error = result.message)
            }
            else -> Unit
        }
    }
}

data class ReportsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    val customFrom: LocalDate = LocalDate.now().minusDays(7),
    val customTo: LocalDate = LocalDate.now(),
    val filterBranchId: String? = null,
    val filterStaffId: String? = null,
    val canFilterBranch: Boolean = true,
    val canFilterStaff: Boolean = true,
    val branches: List<BranchEntity> = emptyList(),
    val data: ReportsData? = null,
    val error: String? = null,
    val showProfit: Boolean = false,
    val canExport: Boolean = false
) {
    val hasActiveFilters: Boolean get() = filterBranchId != null || filterStaffId != null
}
