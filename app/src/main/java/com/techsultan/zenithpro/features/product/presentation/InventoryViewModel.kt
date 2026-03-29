package com.techsultan.zenithpro.features.product.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.product.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.product.domain.use_case.DeleteProductUseCase
import com.techsultan.zenithpro.features.product.domain.use_case.GetProductsUseCase
import com.techsultan.zenithpro.features.product.domain.use_case.SyncProductsUseCase
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
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryUiState())
    val state: StateFlow<InventoryUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<InventoryEvent>()
    val events = _events.asSharedFlow()

    private var businessId: String = ""

    fun init(businessId: String) {
        if (this.businessId == businessId) return
        this.businessId = businessId
        observeProducts()
        observeConnectivity()
        syncOnStart()
    }

    private fun observeProducts() {
        viewModelScope.launch {
            getProductsUseCase(businessId).collect { result ->
                when (result) {
                    is Resource.Loading -> _state.update { it.copy(isLoading = it.products.isEmpty()) }
                    is Resource.Success -> _state.update {
                        it.copy(
                            isLoading = false,
                            products = result.data ?: emptyList(),
                            isEmpty = result.data?.isEmpty() ?: true,
                            error = null
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

    fun onFilterChanged(stockStatus: String, minPrice: Long?, maxPrice: Long?) {
        _state.update {
            it.copy(
                selectedStockStatus = stockStatus,
                minPrice = minPrice,
                maxPrice = maxPrice
            )
        }
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
                    "Out of Stock" -> totalStock == 0
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
        if (businessId.isBlank()) return
        syncProductsUseCase(businessId)
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
    val error: String? = null
)
