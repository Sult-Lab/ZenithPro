package com.techsultan.zenithpro.features.expenses.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
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
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseListUiState())
    val state: StateFlow<ExpenseListUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ExpenseListEvent>()
    val events = _events.asSharedFlow()

    private var businessId = ""
    private var observeJob: Job? = null

    fun init(businessId: String) {
        if (this.businessId == businessId) return
        this.businessId = businessId
        observeExpenses()
        loadStats()
        syncOnStart()
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
        viewModelScope.launch {
            when (val result = deleteExpenseUseCase(expenseId)) {
                is Resource.Success ->
                    _events.emit(ExpenseListEvent.ShowMessage("Expense deleted"))
                is Resource.Error ->
                    _events.emit(ExpenseListEvent.ShowError(result.message ?: "Delete failed"))
                else -> Unit
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            expenseRepository.pullFromServer(businessId)
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
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            getExpensesUseCase(businessId, _state.value.filter).collect { result ->
                when (result) {
                    is Resource.Loading -> _state.update {
                        it.copy(isLoading = it.expenses.isEmpty())
                    }
                    is Resource.Success -> _state.update {
                        it.copy(isLoading = false, expenses = result.data ?: emptyList(), error = null)
                    }
                    is Resource.Error -> _state.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            when (val result = getExpenseStatsUseCase(businessId, _state.value.filter)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        stats      = result.data,
                        categories = result.data?.categories ?: emptyList()
                    )
                }
                else -> Unit
            }
        }
    }

    private fun syncOnStart() {
        viewModelScope.launch {
            if (networkMonitor.isConnected()) {
                expenseRepository.pullFromServer(businessId)
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