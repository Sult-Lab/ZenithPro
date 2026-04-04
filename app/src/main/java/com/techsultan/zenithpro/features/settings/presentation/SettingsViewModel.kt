package com.techsultan.zenithpro.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.settings.data.local.BusinessSettingsEntity
import com.techsultan.zenithpro.features.settings.data.remote.StaffMember
import com.techsultan.zenithpro.features.settings.domain.repository.SettingsRepository
import com.techsultan.zenithpro.features.settings.domain.use_case.GetSettingsUseCase
import com.techsultan.zenithpro.features.settings.domain.use_case.GetStaffListUseCase
import com.techsultan.zenithpro.features.settings.domain.use_case.UpdateSettingsUseCase
import com.techsultan.zenithpro.features.settings.domain.use_case.UpdateStaffRoleUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val getStaffListUseCase: GetStaffListUseCase,
    private val updateStaffRoleUseCase: UpdateStaffRoleUseCase,
    private val settingsRepository: SettingsRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    fun init(businessId: String) {
        viewModelScope.launch {
            getSettingsUseCase(businessId).collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            settingsRepository.pullFromServer(businessId)
        }
        loadStaff(businessId)
    }

    fun loadStaff(businessId: String) {
        viewModelScope.launch {
            when (val result = getStaffListUseCase(businessId)) {
                is Resource.Success -> _state.update { it.copy(staffList = result.data ?: emptyList()) }
                is Resource.Error   -> _state.update { it.copy(error = result.message) }
                else -> Unit
            }
        }
    }

    fun updateSettings(settings: BusinessSettingsEntity) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            when (val result = updateSettingsUseCase(settings)) {
                is Resource.Success -> {
                    _state.update { it.copy(isSaving = false) }
                    _events.emit(SettingsEvent.Saved)
                }
                is Resource.Error -> {
                    _state.update { it.copy(isSaving = false) }
                    _events.emit(SettingsEvent.ShowError(result.message ?: "Failed"))
                }
                else -> Unit
            }
        }
    }

    fun updateStaffRole(userId: String, newRole: String, businessId: String) {
        viewModelScope.launch {
            when (val result = updateStaffRoleUseCase(userId, newRole, businessId)) {
                is Resource.Success -> {
                    loadStaff(businessId)
                    _events.emit(SettingsEvent.RoleUpdated)
                }
                is Resource.Error ->
                    _events.emit(SettingsEvent.ShowError(result.message ?: "Failed"))
                else -> Unit
            }
        }
    }

    fun updateStaffStatus(userId: String, status: String, businessId: String) {
        viewModelScope.launch {
            settingsRepository.updateUserStatus(userId, status)
            loadStaff(businessId)
        }
    }

    sealed class SettingsEvent {
        data object Saved : SettingsEvent()
        data object RoleUpdated : SettingsEvent()
        data class ShowError(val message: String) : SettingsEvent()
    }
}

data class SettingsUiState(
    val settings: BusinessSettingsEntity? = null,
    val staffList: List<StaffMember> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null
)