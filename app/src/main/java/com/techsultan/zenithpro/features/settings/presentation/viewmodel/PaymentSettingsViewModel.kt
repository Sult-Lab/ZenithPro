package com.techsultan.zenithpro.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.settings.data.local.TerminalEntity
import com.techsultan.zenithpro.features.settings.domain.repository.PaymentSettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaymentSettingsViewModel(
    private val repository: PaymentSettingsRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentSettingsUiState())
    val state: StateFlow<PaymentSettingsUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PaymentSettingsEvent>()
    val events = _events.asSharedFlow()

    init {
        observe()
        sync()
    }

    private fun observe() {
        viewModelScope.launch {
            repository.getTerminals(sessionManager.businessId).collect { result ->
                when (result) {
                    is Resource.Success -> _state.update {
                        it.copy(isLoading = false, terminals = result.data ?: emptyList())
                    }
                    is Resource.Error -> _state.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                    is Resource.Loading -> _state.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun sync() {
        viewModelScope.launch {
            repository.pullFromServer(sessionManager.businessId)
        }
    }

    fun onTestModeToggled(enabled: Boolean) {
        _state.update { it.copy(testModeEnabled = enabled) }
    }

    fun onTransferToggled(enabled: Boolean) {
        _state.update { it.copy(transferEnabled = enabled) }
    }

    fun saveSweepAccount(
        terminalId: String,
        bankCode: String,
        accountNumber: String,
        accountName: String
    ) {
        if (accountNumber.length < 10) {
            _state.update { it.copy(sweepError = "Enter a valid 10-digit account number") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, sweepError = null) }
            when (val result = repository.updateSweepAccount(
                terminalId, bankCode, accountNumber, accountName
            )) {
                is Resource.Success -> {
                    _state.update { it.copy(isSaving = false) }
                    _events.emit(PaymentSettingsEvent.SweepSaved)
                }
                is Resource.Error -> {
                    _state.update { it.copy(isSaving = false, sweepError = result.message) }
                }
                else -> _state.update { it.copy(isSaving = false) }
            }
        }
    }

    sealed class PaymentSettingsEvent {
        data object SweepSaved : PaymentSettingsEvent()
    }
}

data class PaymentSettingsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val terminals: List<TerminalEntity> = emptyList(),
    val testModeEnabled: Boolean = false,
    val transferEnabled: Boolean = true,
    val sweepError: String? = null,
    val error: String? = null
)