package com.techsultan.zenithpro.features.expenses.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.ZenithAnalytics
import androidx.core.os.bundleOf
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseEntity
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseFilter
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseStatsData
import com.techsultan.zenithpro.features.expenses.domain.repository.ExpenseRepository
import com.techsultan.zenithpro.features.expenses.domain.use_case.DeleteExpenseUseCase
import com.techsultan.zenithpro.features.expenses.domain.use_case.GetExpenseStatsUseCase
import com.techsultan.zenithpro.features.expenses.domain.use_case.GetExpensesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExpenseListViewModel(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getExpenseStatsUseCase: GetExpenseStatsUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val expenseRepository: ExpenseRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseListUiState())
    val state: StateFlow<ExpenseListUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ExpenseListEvent>()
    val events = _events.asSharedFlow()

    private var businessId: String? = null
    private var observeJob: Job? = null


    init {
        viewModelScope.launch {
            businessId = sessionManager.loadSession()?.businessId
            if (businessId != null) {
                observeExpenses()
                loadStats()
                syncOnStart()
            }
        }
    }

    fun onFilterChanged(filter: ExpenseFilter) {
        _state.update { it.copy(filter = filter) }
        observeExpenses()
        loadStats()
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun deleteExpense(expenseId: String) {
        if (sessionManager.currentSession?.isAdmin != true) {
            viewModelScope.launch {
                _events.emit(ExpenseListEvent.ShowError("Only admins can delete expenses"))
            }
            return
        }
        viewModelScope.launch {
            when (val result = deleteExpenseUseCase(expenseId)) {
                is Resource.Success -> {
                    ZenithAnalytics.trackEvent("expense_deleted")
                    _events.emit(ExpenseListEvent.ShowMessage("Expense deleted"))
                }
                is Resource.Error -> {
                    ZenithAnalytics.logError(Exception(result.message), context = "ExpenseListViewModel.deleteExpense")
                    _events.emit(ExpenseListEvent.ShowError(result.message ?: "Delete failed"))
                }
                else -> Unit
            }
        }
    }

    fun refresh() {
        val bId = businessId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            expenseRepository.pullFromServer(bId)
            loadStats()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    // ── Derived ────────────────────────────────────────────────────

    val filteredExpenses: StateFlow<List<ExpenseEntity>> = state
        .map { s ->
            if (s.searchQuery.isBlank()) s.expenses
            else s.expenses.filter { expense ->
                expense.title.contains(s.searchQuery, ignoreCase = true) ||
                        expense.category.contains(s.searchQuery, ignoreCase = true) ||
                        expense.notes?.contains(s.searchQuery, ignoreCase = true) == true
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun observeExpenses() {
        val bId = businessId ?: return
        val session = sessionManager.currentSession
        if (session?.isStaff == true) {
            _state.update { it.copy(isLoading = false, expenses = emptyList()) }
            return
        }

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            getExpensesUseCase(bId, _state.value.filter).collect { result ->
                when (result) {
                    is Resource.Loading -> _state.update {
                        it.copy(isLoading = it.expenses.isEmpty())
                    }
                    is Resource.Success -> {
                        val allExpenses = result.data ?: emptyList()
                        val filtered = if (session?.isAdmin == true) {
                            allExpenses
                        } else {
                            allExpenses.filter { it.branchId == session?.branchId }
                        }
                        _state.update { it.copy(isLoading = false, expenses = filtered, error = null) }
                    }
                    is Resource.Error -> _state.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    private fun loadStats() {
        val bId = businessId ?: return
        val session = sessionManager.currentSession
        if (session?.isStaff == true) return

        viewModelScope.launch {
            when (val result = getExpenseStatsUseCase(bId, _state.value.filter)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            stats      = result.data,
                            categories = result.data?.categories ?: emptyList()
                        )
                    }
                    ZenithAnalytics.trackEvent("report_viewed", bundleOf(
                        "report_type" to "expenses"
                    ))
                }
                else -> Unit
            }
        }
    }

    private fun syncOnStart() {
        val bId = businessId ?: return
        viewModelScope.launch {
            if (networkMonitor.isConnected()) {
                expenseRepository.pullFromServer(bId)
                loadStats()
            }
        }
    }

    sealed class ExpenseListEvent {
        data class ShowMessage(val message: String) : ExpenseListEvent()
        data class ShowError(val message: String) : ExpenseListEvent()
    }
}

data class ExpenseListUiState(
    val isLoading: Boolean              = false,
    val isRefreshing: Boolean           = false,
    val expenses: List<ExpenseEntity>   = emptyList(),
    val searchQuery: String             = "",
    val filter: ExpenseFilter           = ExpenseFilter(),
    val stats: ExpenseStatsData?        = null,
    val categories: List<String>        = emptyList(),
    val error: String?                  = null
)
