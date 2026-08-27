package com.techsultan.zenithpro.features.customer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.ZenithAnalytics
import androidx.core.os.bundleOf
import com.techsultan.zenithpro.features.customer.domain.repository.CustomerRepository
import com.techsultan.zenithpro.features.customer.domain.use_case.CustomerReportData
import com.techsultan.zenithpro.features.customer.domain.use_case.GetCustomerReportsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CustomerReportsViewModel(
    private val getCustomerReportsUseCase: GetCustomerReportsUseCase,
    private val customerRepository: CustomerRepository,
    private val networkMonitor: NetworkMonitor,
    val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerReportsUiState())
    val state: StateFlow<CustomerReportsUiState> = _state.asStateFlow()

    val session get() = sessionManager.currentSession
    val businessId get() = session?.businessId

    init {
        viewModelScope.launch {
            businessId?.let { load(it) }
        }
    }

    fun load(businessId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            if (networkMonitor.isConnected()) {
                customerRepository.pullFromServer(businessId)
            }
            when (val result = getCustomerReportsUseCase(businessId)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(isLoading = false, data = result.data)
                    }
                    ZenithAnalytics.trackEvent("report_viewed", bundleOf(
                        "report_type" to "customers"
                    ))
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                    ZenithAnalytics.logError(Exception(result.message), context = "CustomerReportsViewModel.load")
                }
                else -> Unit
            }
        }
    }
}

data class CustomerReportsUiState(
    val isLoading: Boolean           = false,
    val data: CustomerReportData?    = null,
    val error: String?               = null
)