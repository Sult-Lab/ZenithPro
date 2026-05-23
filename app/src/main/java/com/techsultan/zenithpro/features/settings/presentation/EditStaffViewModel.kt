package com.techsultan.zenithpro.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util.trimOrNull
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.branch.domain.use_case.GetBranchesUseCase
import com.techsultan.zenithpro.features.settings.data.remote.EditStaffRequest
import com.techsultan.zenithpro.features.settings.data.remote.EditStaffResponse
import com.techsultan.zenithpro.features.settings.data.remote.StaffMember
import com.techsultan.zenithpro.features.settings.domain.use_case.EditStaffUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditStaffViewModel(
    private val editStaffUseCase: EditStaffUseCase,
    private val getBranchesUseCase: GetBranchesUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(EditStaffUiState())
    val state: StateFlow<EditStaffUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<EditStaffEvent>()
    val events = _events.asSharedFlow()

    fun loadStaff(staff: StaffMember) {
        _state.update {
            it.copy(
                staffId = staff.id,
                firstName = staff.firstName,
                lastName = staff.lastName ?: "",
                phone = staff.phone ?: "",
                email = staff.email ?: "",
                role = staff.role,
                branchId = staff.branchId,
                status = staff.status
            )
        }
        loadBranches()
    }

    private fun loadBranches() {
        viewModelScope.launch {
            getBranchesUseCase(sessionManager.businessId).collect { result ->
                if (result is Resource.Success) {
                    _state.update {
                        it.copy(branches = result.data?.filter { b -> b.isActive } ?: emptyList())
                    }
                }
            }
        }
    }

    fun onFirstNameChanged(value: String) {
        _state.update { it.copy(firstName = value, firstNameError = null) }
    }

    fun onLastNameChanged(value: String)  { _state.update { it.copy(lastName = value) } }
    fun onPhoneChanged(value: String)     { _state.update { it.copy(phone = value) } }
    fun onEmailChanged(value: String)     { _state.update { it.copy(email = value) } }

    fun onRoleChanged(role: String) {
        _state.update { it.copy(role = role) }
    }

    fun onBranchChanged(branchId: String?) {
        _state.update { it.copy(branchId = branchId) }
    }

    fun onStatusChanged(status: String) {
        _state.update { it.copy(status = status) }
    }

    fun save() {
        val s = _state.value
        if (s.firstName.isBlank()) {
            _state.update { it.copy(firstNameError = "First name is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = editStaffUseCase(
                EditStaffRequest(
                    staffId = s.staffId,
                    firstName = s.firstName.trim(),
                    lastName = s.lastName.trimOrNull(),
                    phone = s.phone.trimOrNull(),
                    email = s.email.trimOrNull(),
                    role = s.role,
                    branchId = s.branchId,
                    status = s.status
                )
            )

            when (result) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    result.data?.let { _events.emit(EditStaffEvent.Saved(it)) }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                    _events.emit(EditStaffEvent.ShowError(result.message ?: "Failed"))
                }
                else -> _state.update { it.copy(isLoading = false) }
            }
        }
    }

    sealed class EditStaffEvent {
        data class Saved(val staff: EditStaffResponse) : EditStaffEvent()
        data class ShowError(val message: String) : EditStaffEvent()
    }
}

data class EditStaffUiState(
    val staffId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val role: String = "STAFF",
    val branchId: String? = null,
    val status: String = "ACTIVE",
    val branches: List<BranchEntity> = emptyList(),
    val isLoading: Boolean = false,
    val firstNameError: String? = null,
    val error: String? = null
)