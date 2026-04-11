package com.techsultan.zenithpro.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
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
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    private var businessId: String? = null
    var currentUserId: String? = null
        private set
    var isAdmin: Boolean = false
        private set


    init {
        viewModelScope.launch {
            val session = sessionManager.loadSession()
            businessId = session?.businessId
            currentUserId = session?.userId
            isAdmin = session?.isAdmin ?: false

            observeSettings()
            pullFromServer()
            loadStaff()
        }
    }

    private fun observeSettings() {
        val bId = businessId ?: return
        viewModelScope.launch {
            getSettingsUseCase(bId).collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
    }

    private fun pullFromServer() {
        val bId = businessId ?: return
        viewModelScope.launch {
            settingsRepository.pullFromServer(bId)
        }
    }

    fun loadStaff() {
        val bId = businessId ?: return
        viewModelScope.launch {
            when (val result = getStaffListUseCase(bId)) {
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

    fun updateStaffRole(userId: String, newRole: String) {
        val bId = businessId ?: return
        viewModelScope.launch {
            when (val result = updateStaffRoleUseCase(userId, newRole, bId)) {
                is Resource.Success -> {
                    loadStaff()
                    _events.emit(SettingsEvent.RoleUpdated)
                }
                is Resource.Error ->
                    _events.emit(SettingsEvent.ShowError(result.message ?: "Failed"))
                else -> Unit
            }
        }
    }

    fun updateStaffStatus(userId: String, status: String) {
        viewModelScope.launch {
            settingsRepository.updateUserStatus(userId, status)
            loadStaff()
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
