package com.techsultan.zenithpro.features.inventory.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.inventory.domain.use_case.DeleteProductUseCase
import com.techsultan.zenithpro.features.inventory.domain.use_case.GetProductsUseCase
import com.techsultan.zenithpro.features.inventory.domain.use_case.SyncProductsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val syncProductsUseCase: SyncProductsUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryUiState())
    val state: StateFlow<InventoryUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<InventoryEvent>()
    val events = _events.asSharedFlow()

    private var businessId: String? = null
    private var sessionBranchId: String? = null
    private var isAdmin: Boolean = false

    init {
        viewModelScope.launch {
            val session = sessionManager.loadSession() ?: return@launch
            businessId = session.businessId

            observeConnectivity()
            syncOnStart()

            // Observe branch changes and reload products
            sessionManager.activeBranchId.collect { id ->
                sessionBranchId = id
                isAdmin = sessionManager.isAdmin
                _state.update { it.copy(
                    isAdmin = isAdmin,
                    isStaff = sessionManager.currentSession?.isStaff ?: true,
                    selectedBranchId = id,
                )}
                observeProducts()
            }
        }
    }

    fun onFilterChanged(stockStatus: String, minPrice: Long?, maxPrice: Long?) {
        _state.update {
            it.copy(
                selectedStockStatus = stockStatus,
                minPrice = minPrice,
                maxPrice = maxPrice
            )
        }
    }

    private fun observeProducts() {
        val bId = businessId ?: return
        viewModelScope.launch {
            // Determine which branch to scope to
            val branchId = when {
                !isAdmin -> sessionBranchId          // Staff/Manager — their branch
                _state.value.selectedBranchId != null -> _state.value.selectedBranchId // Admin filtered
                else -> null                          // Admin all branches
            }

            val productsFlow = if (branchId != null) {
                getProductsUseCase(bId, branchId)    // branch scoped
            } else {
                getProductsUseCase(bId)              // all branches (admin only)
            }

            productsFlow.collect { result ->
                when (result) {
                    is Resource.Loading -> _state.update {
                        it.copy(isLoading = it.products.isEmpty())
                    }
                    is Resource.Success -> _state.update {
                        it.copy(
                            isLoading = false,
                            products  = result.data ?: emptyList(),
                            isEmpty   = result.data?.isEmpty() ?: true,
                            error     = null
                        )
                    }
                    is Resource.Error -> _state.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            networkMonitor.isConnectedFlow
                .filter { it }
                .collect {
                    Log.d("InventoryVM", "Connectivity restored — syncing")
                    syncProducts()
                }
        }
    }

    private fun syncOnStart() {
        viewModelScope.launch {
            syncProducts()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            syncProducts()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun deleteProduct(productId: String) {
        if (!isAdmin) {
            viewModelScope.launch {
                _events.emit(InventoryEvent.ShowError("Only admins can delete products"))
            }
            return
        }
        viewModelScope.launch {
            val result = deleteProductUseCase(productId)
            if (result is Resource.Error) {
                _events.emit(InventoryEvent.ShowError(result.message ?: "Delete failed"))
            } else {
                _events.emit(InventoryEvent.ProductDeleted)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun onSortOptionChanged(sortOption: SortOption) {
        _state.update { it.copy(sortOption = sortOption) }
    }

    fun onCategoryFilterChanged(category: String?) {
        _state.update { it.copy(selectedCategory = category) }
    }

    fun resetFilters() {
        _state.update {
            it.copy(
                selectedStockStatus = "All items",
                selectedCategory = null,
                minPrice = null,
                maxPrice = null,
                sortOption = SortOption.RECENTLY_ADDED
            )
        }
    }

    val filteredProducts: StateFlow<List<ProductWithVariants>> =
        state.map { s ->
            s.products.filter { p ->
                val matchesSearch = s.searchQuery.isBlank() ||
                        p.product.name.contains(s.searchQuery, ignoreCase = true) ||
                        p.product.category?.contains(s.searchQuery, ignoreCase = true) == true

                val totalStock = p.variants.sumOf { v -> v.stock.sumOf { it.quantity } }
                val matchesStockStatus = when (s.selectedStockStatus) {
                    "In Stock" -> totalStock > 0
                    "Low Stock" -> p.variants.any { v -> v.stock.any { it.quantity <= (it.lowStockAlert ?: 5) && it.quantity > 0 } }
                    "Out of Stock" -> totalStock == 0.0
                    else -> true
                }

                val matchesCategory = s.selectedCategory == null ||
                        p.product.category == s.selectedCategory

                val price = p.product.baseSalesPrice
                val matchesPrice = (s.minPrice == null || price >= s.minPrice) &&
                        (s.maxPrice == null || price <= s.maxPrice)

                matchesSearch && matchesStockStatus && matchesCategory && matchesPrice
            }.let { filteredList ->
                when (s.sortOption) {
                    SortOption.NAME_ASC -> filteredList.sortedBy { it.product.name }
                    SortOption.NAME_DESC -> filteredList.sortedByDescending { it.product.name }
                    SortOption.QUANTITY_LOW_HIGH -> filteredList.sortedBy { it.variants.sumOf { v -> v.stock.sumOf { s -> s.quantity } } }
                    SortOption.QUANTITY_HIGH_LOW -> filteredList.sortedByDescending { it.variants.sumOf { v -> v.stock.sumOf { s -> s.quantity } } }
                    SortOption.PRICE_LOW_HIGH -> filteredList.sortedBy { it.product.baseSalesPrice }
                    SortOption.PRICE_HIGH_LOW -> filteredList.sortedByDescending { it.product.baseSalesPrice }
                    SortOption.RECENTLY_ADDED -> filteredList.sortedByDescending { it.product.updatedAt }
                    SortOption.EXPIRY_DATE -> filteredList
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private suspend fun syncProducts() {
        val bId = businessId ?: return
        syncProductsUseCase(bId)
    }

    sealed class InventoryEvent {
        data object ProductDeleted : InventoryEvent()
        data class ShowError(val message: String) : InventoryEvent()
    }
}

enum class SortOption(val title: String) {
    NAME_ASC("Product Name (A-Z)"),
    NAME_DESC("Product Name (Z-A)"),
    QUANTITY_LOW_HIGH("Quantity (Low to High)"),
    QUANTITY_HIGH_LOW("Quantity (High to Low)"),
    PRICE_LOW_HIGH("Price (Low to High)"),
    PRICE_HIGH_LOW("Price (High to Low)"),
    RECENTLY_ADDED("Recently Added"),
    EXPIRY_DATE("Expiry Date")
}

data class InventoryUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val products: List<ProductWithVariants> = emptyList(),
    val isEmpty: Boolean = false,
    val searchQuery: String = "",
    val selectedStockStatus: String = "All items",
    val selectedCategory: String? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val sortOption: SortOption = SortOption.RECENTLY_ADDED,
    val error: String? = null,
    // Branch filter — only relevant for ADMIN
    val branches: List<BranchEntity> = emptyList(),
    val selectedBranchId: String? = null,   // null = all branches
    val isAdmin: Boolean = false,
    val isStaff: Boolean = true,
)
