package com.techsultan.zenithpro.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util.trimOrNull
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.branch.domain.use_case.GetBranchesUseCase
import com.techsultan.zenithpro.features.settings.data.remote.CreateStaffRequest
import com.techsultan.zenithpro.features.settings.data.remote.CreateStaffResponse
import com.techsultan.zenithpro.features.settings.domain.use_case.CreateStaffUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateStaffViewModel(
    private val createStaffUseCase: CreateStaffUseCase,
    private val getBranchesUseCase: GetBranchesUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateStaffUiState())
    val state: StateFlow<CreateStaffUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CreateStaffEvent>()
    val events = _events.asSharedFlow()

    init {
        loadBranches()
        loadSession()
    }

    private fun loadSession() {
        val session = sessionManager.currentSession
        _state.update { it.copy(
            currentRole = session?.role ?: "STAFF",
            selectedRole = "STAFF", // Default
            selectedBranchId = if (session?.isAdmin == false) session.branchId else null
        ) }
    }

    private fun loadBranches() {
        viewModelScope.launch {
            getBranchesUseCase(sessionManager.businessId).collect { result ->
                if (result is Resource.Success) {
                    val branches = result.data ?: emptyList()
                    val session = sessionManager.currentSession
                    val filteredBranches = if (session?.isAdmin == true) {
                        branches
                    } else {
                        branches.filter { it.id == session?.branchId }
                    }
                    _state.update { it.copy(branches = filteredBranches) }
                }
            }
        }
    }

    fun onEmailChanged(value: String) {
        _state.update { it.copy(email = value, emailError = null) }
    }

    fun onPasswordChanged(value: String) {
        _state.update { it.copy(password = value, passwordError = null) }
    }

    fun onFirstNameChanged(value: String) {
        _state.update { it.copy(firstName = value, firstNameError = null) }
    }

    fun onLastNameChanged(value: String) {
        _state.update { it.copy(lastName = value) }
    }

    fun onPhoneChanged(value: String) {
        _state.update { it.copy(phone = value) }
    }

    fun onRoleChanged(role: String) {
        _state.update { it.copy(selectedRole = role) }
    }

    fun onBranchChanged(branchId: String?) {
        _state.update { it.copy(selectedBranchId = branchId) }
    }

    fun createStaff() {
        val s = _state.value

        // Local validation
        var hasError = false
        if (s.email.isBlank()) {
            _state.update { it.copy(emailError = "Email is required") }
            hasError = true
        }
        if (s.password.length < 6) {
            _state.update { it.copy(passwordError = "Minimum 6 characters") }
            hasError = true
        }
        if (s.firstName.isBlank()) {
            _state.update { it.copy(firstNameError = "First name is required") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = createStaffUseCase(
                CreateStaffRequest(
                    email     = s.email.trim(),
                    password  = s.password,
                    firstName = s.firstName.trim(),
                    lastName  = s.lastName.trimOrNull(),
                    phone     = s.phone.trimOrNull(),
                    role      = s.selectedRole,
                    branchId  = s.selectedBranchId
                )
            )

            when (result) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    result.data?.let { _events.emit(CreateStaffEvent.Created(it)) }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                    _events.emit(CreateStaffEvent.ShowError(result.message ?: "Failed"))
                }
                else -> Unit
            }
        }
    }

    sealed class CreateStaffEvent {
        data class Created(val staff: CreateStaffResponse) : CreateStaffEvent()
        data class ShowError(val message: String) : CreateStaffEvent()
    }
}

data class CreateStaffUiState(
    val email: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val selectedRole: String = "STAFF",
    val currentRole: String = "STAFF",
    val selectedBranchId: String? = null,
    val branches: List<BranchEntity> = emptyList(),
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val firstNameError: String? = null,
    val error: String? = null
)