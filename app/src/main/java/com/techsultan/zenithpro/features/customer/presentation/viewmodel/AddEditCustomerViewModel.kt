package com.techsultan.zenithpro.features.customer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.ZenithAnalytics
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.data.remote.CustomerRequest
import com.techsultan.zenithpro.features.customer.domain.use_case.UpsertCustomerUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddEditCustomerViewModel(
    private val upsertCustomerUseCase: UpsertCustomerUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditCustomerUiState())
    val state: StateFlow<AddEditCustomerUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AddEditCustomerEvent>()
    val events = _events.asSharedFlow()

    private var businessId: String? = null

    init {
        viewModelScope.launch {
            businessId = sessionManager.loadSession()?.businessId
        }
    }

    // Pre-fill form when editing
    fun loadCustomer(customer: CustomerEntity) {
        _state.update {
            it.copy(
                customerId  = customer.id,
                firstName   = customer.firstName,
                lastName    = customer.lastName ?: "",
                phone       = customer.phone ?: "",
                email       = customer.email ?: "",
                address     = customer.address ?: "",
                notes       = customer.notes ?: "",
                isEditMode  = true
            )
        }
    }

    fun onFirstNameChanged(value: String)  { _state.update { it.copy(firstName = value) } }
    fun onLastNameChanged(value: String)   { _state.update { it.copy(lastName = value) } }
    fun onPhoneChanged(value: String)      { _state.update { it.copy(phone = value) } }
    fun onEmailChanged(value: String)      { _state.update { it.copy(email = value) } }
    fun onAddressChanged(value: String)    { _state.update { it.copy(address = value) } }
    fun onNotesChanged(value: String)      { _state.update { it.copy(notes = value) } }

    fun save() {
        val s = _state.value
        if (s.firstName.isBlank()) {
            _state.update { it.copy(firstNameError = "First name is required") }
            return
        }

        viewModelScope.launch {
            val bId = businessId ?: sessionManager.loadSession()?.businessId
            if (bId == null) {
                _events.emit(AddEditCustomerEvent.ShowError("Session expired. Please login again."))
                return@launch
            }
            businessId = bId

            _state.update { it.copy(isLoading = true, firstNameError = null) }

            val result = upsertCustomerUseCase(
                request = CustomerRequest(
                    id        = s.customerId,
                    firstName = s.firstName.trim(),
                    lastName  = s.lastName.trim(),
                    phone     = s.phone.trim(),
                    email     = s.email.trim(),
                    address   = s.address.trim(),
                    notes     = s.notes.trim()
                ),
                businessId = bId
            )

            when (result) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    if (!s.isEditMode) {
                        ZenithAnalytics.trackEvent("customer_created")
                    }
                    _events.emit(AddEditCustomerEvent.Saved(result.data?.id ?: ""))
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                    _events.emit(AddEditCustomerEvent.ShowError(result.message ?: "Save failed"))
                }
                else -> Unit
            }
        }
    }

    sealed class AddEditCustomerEvent {
        data class Saved(val customerId: String) : AddEditCustomerEvent()
        data class ShowError(val message: String) : AddEditCustomerEvent()
    }
}

data class AddEditCustomerUiState(
    val customerId: String?  = null,
    val firstName: String    = "",
    val lastName: String     = "",
    val phone: String        = "",
    val email: String        = "",
    val address: String      = "",
    val notes: String        = "",
    val isEditMode: Boolean  = false,
    val isLoading: Boolean   = false,
    val firstNameError: String? = null,
    val error: String?       = null
)
