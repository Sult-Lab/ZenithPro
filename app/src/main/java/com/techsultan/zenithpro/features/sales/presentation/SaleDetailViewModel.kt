package com.techsultan.zenithpro.features.sales.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.features.sales.data.local.SaleDao
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SaleDetailViewModel(
    private val saleDao: SaleDao,
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

        }
    }

    sealed class SaleDetailEvent {
        data object PaymentConfirmed : SaleDetailEvent()
    }
}

data class SaleDetailUiState(
    val saleWithItems: SaleWithItems? = null,
    val isPolling: Boolean            = false
)