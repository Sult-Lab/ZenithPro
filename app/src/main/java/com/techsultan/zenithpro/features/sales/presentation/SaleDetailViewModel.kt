package com.techsultan.zenithpro.features.sales.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.sales.FulfillmentStatus
import com.techsultan.zenithpro.features.sales.data.local.SaleDao
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import com.techsultan.zenithpro.features.sales.domain.repository.SaleRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SaleDetailViewModel(
    private val saleDao: SaleDao,
    private val saleRepository: SaleRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SaleDetailUiState())
    val state: StateFlow<SaleDetailUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<SaleDetailEvent>()
    val events = _events.asSharedFlow()

    private var saleId = ""

    fun load(saleId: String, businessId: String) {
        this.saleId = saleId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val sale = saleDao.getSaleById(saleId)
                val session = sessionManager.loadSession()
                val cashierName = session?.firstName ?: "Staff"

                // TODO: Confirm role-restriction rules for fulfillment status changes with business owner.
                // Defaulting to MANAGER + ADMIN only (STAFF cannot update fulfillment status).
                val canUpdateFulfillment = session?.isManager == true || session?.isAdmin == true

                _state.update {
                    it.copy(
                        saleWithItems = sale,
                        cashierName = cashierName,
                        canUpdateFulfillment = canUpdateFulfillment,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun updateFulfillmentStatus(newStatus: FulfillmentStatus, notes: String? = null) {
        viewModelScope.launch {
            val session = sessionManager.loadSession() ?: run {
                _events.emit(SaleDetailEvent.ShowError("Session expired"))
                return@launch
            }
            if (!session.isManager && !session.isAdmin) {
                _events.emit(SaleDetailEvent.ShowError("Only Managers and Admins can update fulfillment status"))
                return@launch
            }
            val currentSale = _state.value.saleWithItems?.sale ?: return@launch
            if (currentSale.fulfillmentStatus in listOf(FulfillmentStatus.DELIVERED, FulfillmentStatus.FAILED, FulfillmentStatus.CANCELLED)) {
                _events.emit(SaleDetailEvent.ShowError("Terminal status cannot be modified"))
                return@launch
            }

            _state.update { it.copy(isUpdating = true) }
            when (val result = saleRepository.updateFulfillmentStatus(
                saleId = currentSale.id,
                newStatus = newStatus,
                changedBy = session.userId,
                notes = notes
            )) {
                is Resource.Success -> {
                    _state.update { it.copy(isUpdating = false) }
                    _events.emit(SaleDetailEvent.StatusUpdated)
                    load(currentSale.id, session.businessId)
                }
                is Resource.Error -> {
                    _state.update { it.copy(isUpdating = false) }
                    _events.emit(SaleDetailEvent.ShowError(result.message ?: "Failed to update status"))
                }
                else -> Unit
            }
        }
    }

    sealed class SaleDetailEvent {
        data object PaymentConfirmed : SaleDetailEvent()
        data object StatusUpdated : SaleDetailEvent()
        data class ShowError(val message: String) : SaleDetailEvent()
    }
}

data class SaleDetailUiState(
    val saleWithItems: SaleWithItems? = null,
    val cashierName: String           = "",
    val canUpdateFulfillment: Boolean = false,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val error: String? = null
)
