package com.techsultan.zenithpro.features.customer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.domain.repository.CustomerRepository
import com.techsultan.zenithpro.features.customer.domain.use_case.GetCustomersUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class CustomerListViewModel(
    private val getCustomersUseCase: GetCustomersUseCase,
    private val customerRepository: CustomerRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerListUiState())
    val state: StateFlow<CustomerListUiState> = _state.asStateFlow()

    private var businessId: String? = null
    private var searchJob: Job? = null


    init {
        viewModelScope.launch {
            businessId = sessionManager.loadSession()?.businessId
            if (businessId != null) {
                observeCustomers()
                syncOnStart()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)   // debounce
            observeCustomers()
        }
    }

    fun onTabChanged(tab: CustomerTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun refresh() {
        val bId = businessId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            customerRepository.pullFromServer(bId)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun observeCustomers() {
        val bId = businessId ?: return
        viewModelScope.launch {
            getCustomersUseCase(bId, _state.value.searchQuery)
                .collect { result ->
                    when (result) {
                        is Resource.Loading -> _state.update {
                            it.copy(isLoading = it.customers.isEmpty())
                        }
                        is Resource.Success -> _state.update {
                            it.copy(
                                isLoading = false,
                                customers = result.data ?: emptyList(),
                                error     = null
                            )
                        }
                        is Resource.Error -> _state.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                    }
                }
        }

        // Debt list tab
        viewModelScope.launch {
            customerRepository.getCustomersWithDebt(bId)
                .collect { result ->
                    if (result is Resource.Success) {
                        _state.update { it.copy(debtors = result.data ?: emptyList()) }
                    }
                }
        }
    }

    private fun syncOnStart() {
        val bId = businessId ?: return
        viewModelScope.launch {
            if (networkMonitor.isConnected()) {
                customerRepository.pullFromServer(bId)
            }
        }
    }
}

data class CustomerListUiState(
    val isLoading: Boolean              = false,
    val isRefreshing: Boolean           = false,
    val customers: List<CustomerEntity> = emptyList(),
    val debtors: List<CustomerEntity>   = emptyList(),
    val searchQuery: String             = "",
    val selectedTab: CustomerTab        = CustomerTab.ALL,
    val error: String?                  = null
)

enum class CustomerTab { ALL, DEBTORS }
