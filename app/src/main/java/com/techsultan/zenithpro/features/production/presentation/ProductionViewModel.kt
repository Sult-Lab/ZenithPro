package com.techsultan.zenithpro.features.production.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.data.UserSession
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.branch.data.local.BranchDao
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.inventory.domain.use_case.GetProductsUseCase
import com.techsultan.zenithpro.features.material.data.local.MaterialEntity
import com.techsultan.zenithpro.features.material.data.local.RecipeWithMaterial
import com.techsultan.zenithpro.features.material.domain.repository.MaterialRepository
import com.techsultan.zenithpro.features.production.data.local.ProductionOrderEntity
import com.techsultan.zenithpro.features.production.domain.repository.ProductionRepository
import com.techsultan.zenithpro.features.production.domain.use_case.CompleteProductionOrderUseCase
import com.techsultan.zenithpro.features.production.domain.use_case.CreateProductionOrderUseCase
import com.techsultan.zenithpro.features.production.domain.use_case.GetProductionOrdersUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

class ProductionViewModel(
    private val getOrdersUseCase: GetProductionOrdersUseCase,
    private val createOrderUseCase: CreateProductionOrderUseCase,
    private val completeOrderUseCase: CompleteProductionOrderUseCase,
    private val productionRepository: ProductionRepository,
    private val materialRepository: MaterialRepository,
    private val getProductsUseCase: GetProductsUseCase,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager,
    private val branchDao: BranchDao
) : ViewModel() {

    private val _state = MutableStateFlow(ProductionUiState())
    val state: StateFlow<ProductionUiState> = _state.asStateFlow()

    private val selectedRecipeVariantId = MutableStateFlow<String?>(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    val recipeMaterials: StateFlow<List<RecipeWithMaterial>> = selectedRecipeVariantId
        .flatMapLatest { variantId ->
            if (variantId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                materialRepository.getRecipeForVariant(variantId)
                    .map { resource -> (resource as? Resource.Success)?.data ?: emptyList() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onProductSelectedForRecipe(variantId: String) {
        selectedRecipeVariantId.value = variantId
    }

    fun clearRecipeSelection() {
        selectedRecipeVariantId.value = null
    }

    private val _events = MutableSharedFlow<ProductionEvent>()
    val events = _events.asSharedFlow()

    private var session: UserSession? = null
    private var businessId: String? = null

    init {
        viewModelScope.launch {
            val currentSession = sessionManager.loadSession()
            session = currentSession
            businessId = currentSession?.businessId
            if (currentSession != null && businessId != null) {
                _state.update { it.copy(
                    isAdmin = currentSession.isAdmin,
                    activeBranchId = sessionManager.activeBranchId.value,
                    activeBranchName = sessionManager.activeBranchName.value
                ) }

                launch {
                    sessionManager.activeBranchId.collect { id ->
                        _state.update { it.copy(
                            activeBranchId = id,
                            activeBranchName = sessionManager.activeBranchName.value
                        ) }
                    }
                }

                launch {
                    branchDao.getBranches(businessId!!).collect { list ->
                        val activeBranches = list.filter { it.isActive }
                        _state.update { it.copy(branches = activeBranches) }
                    }
                }

                init(businessId)
            }
        }
    }

    fun init(businessId: String?) {
        val bId = businessId ?: return
        viewModelScope.launch {
            getOrdersUseCase(bId).collect { result ->
                when (result) {
                    is Resource.Success -> _state.update {
                        it.copy(isLoading = false, orders = result.data ?: emptyList())
                    }
                    is Resource.Error -> _state.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                    is Resource.Loading -> _state.update { it.copy(isLoading = true) }
                }
            }
        }
        viewModelScope.launch {
            getProductsUseCase(bId).collect { result ->
                if (result is Resource.Success) {
                    _state.update { it.copy(products = result.data ?: emptyList()) }
                }
            }
        }
        viewModelScope.launch {
            materialRepository.getMaterials(bId).collect { result ->
                if (result is Resource.Success) {
                    _state.update { it.copy(materials = result.data ?: emptyList()) }
                }
            }
        }
        viewModelScope.launch {
            if (networkMonitor.isConnected()) {
                productionRepository.pullFromServer(bId)
                materialRepository.pullFromServer(bId)
            }
        }
    }

    fun refresh() {
        val bId = businessId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            if (networkMonitor.isConnected()) {
                productionRepository.pullFromServer(bId)
                materialRepository.pullFromServer(bId)
            }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun onBranchSelected(branchId: String?, branchName: String?) {
        viewModelScope.launch {
            sessionManager.setActiveBranch(branchId, branchName)
        }
    }

    fun onStatusFilterChanged(status: String?) {
        _state.update { it.copy(selectedStatusFilter = status) }
        val bId = businessId ?: return
        viewModelScope.launch {
            getOrdersUseCase(bId, status).collect { result ->
                if (result is Resource.Success) {
                    _state.update { it.copy(orders = result.data ?: emptyList()) }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun createOrder(
        variantId: String,
        quantity: Double,
        notes: String?
    ) {
        viewModelScope.launch {
            val bId = businessId ?: return@launch
            val currentSession = session ?: return@launch

            _state.update { it.copy(isSaving = true) }
            when (val result = createOrderUseCase(
                variantId,
                quantity,
                currentSession.branchId,
                notes,
                bId,
                currentSession.userId
            )) {
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

    fun startOrder(orderId: String, overriddenItems: List<Pair<String, Double>>? = null) {
        viewModelScope.launch {
            when (val result = productionRepository.startOrder(orderId, overriddenItems)) {
                is Resource.Error -> _events.emit(ProductionEvent.ShowError(result.message ?: "Failed to start"))
                else -> Unit
            }
        }
    }

    fun completeOrder(
        orderId: String,
        actualQuantity: Double,
        remarks: String? = null,
        customerId: String? = null,
        unitPrice: Long? = null,
        amountPaid: Long? = null,
        paymentMethod: String? = "CASH"
    ) {
        viewModelScope.launch {
            val currentSession = session ?: run {
                _events.emit(ProductionEvent.ShowError("Session expired"))
                return@launch
            }
            val activeBranchId = sessionManager.activeBranchId.value ?: run {
                _events.emit(ProductionEvent.ShowError("Branch ID is required to complete production"))
                return@launch
            }

            _state.update { it.copy(isCompleting = true) }
            when (val result = completeOrderUseCase(
                orderId = orderId,
                branchId = activeBranchId,
                actualQuantity = actualQuantity,
                completedBy = currentSession.userId,
                completedByName = currentSession.fullName,
                completedByRole = currentSession.role,
                customerId = customerId,
                terminalId = currentSession.terminalId,
                unitPrice = unitPrice,
                amountPaid = amountPaid,
                paymentMethod = paymentMethod,
                remarks = remarks
            )) {
                is Resource.Success -> {
                    _state.update { it.copy(isCompleting = false) }
                    _events.emit(ProductionEvent.OrderCompleted)
                }
                is Resource.Error -> {
                    _state.update { it.copy(isCompleting = false) }
                    _events.emit(ProductionEvent.ShowError(result.message ?: "Failed to complete order"))
                }
                else -> Unit
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            when (val result = productionRepository.cancelOrder(orderId)) {
                is Resource.Error -> _events.emit(ProductionEvent.ShowError(result.message ?: "Failed to cancel"))
                else -> Unit
            }
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
    val isRefreshing: Boolean = false,
    val isSaving: Boolean = false,
    val isCompleting: Boolean = false,
    val orders: List<ProductionOrderEntity> = emptyList(),
    val products: List<ProductWithVariants> = emptyList(),
    val materials: List<MaterialEntity> = emptyList(),
    val selectedStatusFilter: String? = null,
    val searchQuery: String = "",
    val error: String? = null,
    val isAdmin: Boolean = false,
    val branches: List<BranchEntity> = emptyList(),
    val activeBranchId: String? = null,
    val activeBranchName: String? = null
)
