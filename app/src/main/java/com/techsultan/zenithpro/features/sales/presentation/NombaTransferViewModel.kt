package com.techsultan.zenithpro.features.sales.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.sales.domain.model.NombaSummary
import com.techsultan.zenithpro.features.sales.domain.model.NombaTransfer
import com.techsultan.zenithpro.features.sales.domain.use_case.GetNombaSummaryUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.GetNombaTransfersUseCase
import com.techsultan.zenithpro.features.sales.domain.repository.NombaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class NombaTransferUiState(
    val summary: NombaSummary? = null,
    val transfers: List<NombaTransfer> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
)

class NombaTransferViewModel(
    private val getNombaTransfersUseCase: GetNombaTransfersUseCase,
    private val getNombaSummaryUseCase: GetNombaSummaryUseCase,
    private val nombaRepository: NombaRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NombaTransferUiState())
    val uiState: StateFlow<NombaTransferUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val businessId = sessionManager.businessId
        
        getNombaSummaryUseCase(businessId).onEach { result ->
            when (result) {
                is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    summary = result.data
                )
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }.launchIn(viewModelScope)

        getNombaTransfersUseCase(businessId).onEach { result ->
            when (result) {
                is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    transfers = result.data ?: emptyList()
                )
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }.launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            nombaRepository.refreshTransfers(sessionManager.businessId)
            loadData()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun confirmTransfer(transferId: String) {
        viewModelScope.launch {
            val result = nombaRepository.confirmTransfer(transferId)
            if (result is Resource.Success) {
                // Refresh transfers so confirmed one shows SUCCESS
                loadData()
            } else if (result is Resource.Error) {
                _uiState.value = _uiState.value.copy(
                    error = result.message
                )
            }
        }
    }
}
