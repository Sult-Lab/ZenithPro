package com.techsultan.zenithpro.features.production.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.material.domain.repository.MaterialRepository
import com.techsultan.zenithpro.features.production.data.local.ProductionOrderEntity
import com.techsultan.zenithpro.features.production.domain.repository.ProductionRepository
import com.techsultan.zenithpro.features.production.domain.use_case.CompleteProductionOrderUseCase
import com.techsultan.zenithpro.features.production.domain.use_case.CreateProductionOrderUseCase
import com.techsultan.zenithpro.features.production.domain.use_case.GetProductionOrdersUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductionViewModel(
    private val getOrdersUseCase: GetProductionOrdersUseCase,
    private val createOrderUseCase: CreateProductionOrderUseCase,
    private val completeOrderUseCase: CompleteProductionOrderUseCase,
    private val productionRepository: ProductionRepository,
    private val materialRepository: MaterialRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(ProductionUiState())
    val state: StateFlow<ProductionUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ProductionEvent>()
    val events = _events.asSharedFlow()

    val session = sessionManager.currentSession!!
    private var businessId: String? = null

    init {
      viewModelScope.launch {
          businessId = sessionManager.loadSession()?.businessId
          if (businessId != null){
              init(businessId)
          }
      }
    }

    fun init(businessId: String?) {
        viewModelScope.launch {
            val bId = businessId ?: return@launch
            getOrdersUseCase(bId).collect { result ->
                when (result) {
                    is Resource.Success -> _state.update {
                        it.copy(isLoading = false, orders = result.data ?: emptyList())
                    }
                    is Resource.Error   -> _state.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                    is Resource.Loading -> _state.update { it.copy(isLoading = true) }
                }
            }
        }
        viewModelScope.launch {
            val bId = businessId ?: return@launch
            if (networkMonitor.isConnected()) {
                productionRepository.pullFromServer(bId)
                materialRepository.pullFromServer(bId)
            }
        }
    }

    fun createOrder(
        variantId: String,
        quantity: Double,
        notes: String?
    ) {
        viewModelScope.launch {
            val bId = businessId ?: return@launch
            _state.update { it.copy(isSaving = true) }
            when (val result = createOrderUseCase(
                variantId, quantity, session.branchId, notes, bId, session.userId)
            ) {
                is Resource.Success -> {
                    _state.update { it.copy(isSaving = false) }
                    _events.emit(ProductionEvent.OrderCreated)
                }
                is Resource.Error -> {
                    _state.update { it.copy(isSaving = false) }
                    _events.emit(ProductionEvent.ShowError(result.message ?: "Failed"))
                }
                else -> Unit
            }
        }
    }

    fun startOrder(orderId: String) {
        viewModelScope.launch {
            productionRepository.startOrder(orderId)
        }
    }

    fun completeOrder(orderId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isCompleting = true) }
            when (val result = completeOrderUseCase(orderId, session.businessId, session.userId)) {
                is Resource.Success -> {
                    _state.update { it.copy(isCompleting = false) }
                    _events.emit(ProductionEvent.OrderCompleted)
                }
                is Resource.Error -> {
                    _state.update { it.copy(isCompleting = false) }
                    _events.emit(ProductionEvent.ShowError(result.message ?: "Failed"))
                }
                else -> Unit
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            productionRepository.cancelOrder(orderId)
        }
    }

    sealed class ProductionEvent {
        data object OrderCreated : ProductionEvent()
        data object OrderCompleted : ProductionEvent()
        data class ShowError(val message: String) : ProductionEvent()
    }
}

data class ProductionUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isCompleting: Boolean = false,
    val orders: List<ProductionOrderEntity> = emptyList(),
    val error: String? = null
)