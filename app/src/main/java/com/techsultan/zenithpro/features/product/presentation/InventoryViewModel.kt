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

    // In production this comes from your session/auth manager
    // Injected or passed from the nav graph
    private var businessId: String = ""

    fun init(businessId: String) {
        if (this.businessId == businessId) return  // already initialised for this business
        this.businessId = businessId
        observeProducts()
        observeConnectivity()
        syncOnStart()
    }

    // ── Observe Room — UI always reflects local state immediately ──────────

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

    // ── Auto-sync when connectivity is restored ────────────────────────────

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

    // ── Initial sync on screen entry ───────────────────────────────────────

    private fun syncOnStart() {
        viewModelScope.launch {
            syncProducts()
        }
    }

    // ── Manual refresh (pull-to-refresh gesture) ───────────────────────────

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            syncProducts()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────

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

    // ── Search / filter (local, no network needed) ─────────────────────────

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun onCategoryFilterChanged(category: String?) {
        _state.update { it.copy(selectedCategory = category) }
    }

    // Derived filtered list — pure local computation, no Room query needed
    val filteredProducts: StateFlow<List<ProductWithVariants>> =
        state.map { s ->
            s.products.filter { p ->
                val matchesSearch = s.searchQuery.isBlank() ||
                        p.product.name.contains(s.searchQuery, ignoreCase = true) ||
                        p.product.category?.contains(s.searchQuery, ignoreCase = true) == true

                val matchesCategory = s.selectedCategory == null ||
                        p.product.category == s.selectedCategory

                matchesSearch && matchesCategory
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Private ────────────────────────────────────────────────────────────

    private suspend fun syncProducts() {
        if (businessId.isBlank()) return
        syncProductsUseCase(businessId)
    }

    // ── Events ─────────────────────────────────────────────────────────────

    sealed class InventoryEvent {
        data object ProductDeleted : InventoryEvent()
        data class ShowError(val message: String) : InventoryEvent()
    }
}

data class InventoryUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val products: List<ProductWithVariants> = emptyList(),
    val isEmpty: Boolean = false,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val error: String? = null
)
