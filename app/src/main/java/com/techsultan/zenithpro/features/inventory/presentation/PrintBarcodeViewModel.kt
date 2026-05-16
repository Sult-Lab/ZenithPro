package com.techsultan.zenithpro.features.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.data.local.PrinterDataStore
import com.techsultan.zenithpro.core.domain.repository.PrinterRepository
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.BarcodeLabelFormatter
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.inventory.domain.use_case.GetProductsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PrintBarcodeViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val sessionManager: SessionManager,
    private val printerRepository: PrinterRepository,
    private val printerDataStore: PrinterDataStore,
    private val barcodeLabelFormatter: BarcodeLabelFormatter
) : ViewModel() {

    private val _state = MutableStateFlow(PrintBarcodeUiState())
    val state: StateFlow<PrintBarcodeUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PrintBarcodeEvent>()
    val events = _events.asSharedFlow()

    private var allProducts: List<ProductWithVariants> = emptyList()

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            val businessId = sessionManager.loadSession()?.businessId ?: return@launch
            getProductsUseCase(businessId).collect { result ->
                when (result) {
                    is Resource.Loading -> _state.update { it.copy(isLoading = true) }
                    is Resource.Success -> {
                        allProducts = result.data ?: emptyList()
                        _state.update { it.copy(isLoading = false, products = allProducts) }
                    }
                    is Resource.Error -> _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
        filterProducts()
    }

    fun onTabSelected(tab: PrintBarcodeTab) {
        _state.update { it.copy(selectedTab = tab) }
        filterProducts()
    }

    fun toggleProductSelection(productId: String) {
        _state.update { state ->
            val newSelected = state.selectedProductIds.toMutableSet()
            if (newSelected.contains(productId)) {
                newSelected.remove(productId)
            } else {
                newSelected.add(productId)
            }
            state.copy(selectedProductIds = newSelected)
        }
        if (_state.value.selectedTab == PrintBarcodeTab.SELECTED) {
            filterProducts()
        }
    }

    fun updateCopies(count: Int) {
        if (count >= 1) {
            _state.update { it.copy(copiesPerItem = count) }
        }
    }

    fun printBarcodes() {
        viewModelScope.launch {
            val printer = printerDataStore.savedPrinter.firstOrNull()
            if (printer == null) {
                _events.emit(PrintBarcodeEvent.ShowError("No printer configured. Please go to settings."))
                return@launch
            }

            val selectedProducts = allProducts.filter { state.value.selectedProductIds.contains(it.product.id) }
            if (selectedProducts.isEmpty()) {
                _events.emit(PrintBarcodeEvent.ShowError("No products selected"))
                return@launch
            }

            _state.update { it.copy(isPrinting = true) }

            try {
                selectedProducts.forEach { product ->
                    val label = barcodeLabelFormatter.format(product)
                    repeat(state.value.copiesPerItem) {
                        printerRepository.printCustom(label, printer).getOrThrow()
                    }
                }
                _events.emit(PrintBarcodeEvent.PrintSuccess)
            } catch (e: Exception) {
                _events.emit(PrintBarcodeEvent.ShowError("Print failed: ${e.message}"))
            } finally {
                _state.update { it.copy(isPrinting = false) }
            }
        }
    }

    private fun filterProducts() {
        val currentState = _state.value
        val filtered = allProducts.filter { p ->
            val matchesSearch = currentState.searchQuery.isBlank() ||
                    p.product.name.contains(currentState.searchQuery, ignoreCase = true) ||
                    p.variants.any { it.variant.sku.contains(currentState.searchQuery, ignoreCase = true) }

            val matchesTab = when (currentState.selectedTab) {
                PrintBarcodeTab.ALL -> true
                PrintBarcodeTab.SELECTED -> currentState.selectedProductIds.contains(p.product.id)
                PrintBarcodeTab.LOW_STOCK -> p.variants.any { v -> 
                    v.stock.any { it.quantity <= (it.lowStockAlert ?: 5) }
                }
            }
            matchesSearch && matchesTab
        }
        _state.update { it.copy(products = filtered) }
    }
}

data class PrintBarcodeUiState(
    val isLoading: Boolean = false,
    val isPrinting: Boolean = false,
    val products: List<ProductWithVariants> = emptyList(),
    val selectedProductIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedTab: PrintBarcodeTab = PrintBarcodeTab.ALL,
    val copiesPerItem: Int = 1,
    val selectedTemplate: String = "Standard 30mm x 20mm"
)

enum class PrintBarcodeTab {
    ALL, SELECTED, LOW_STOCK
}

sealed class PrintBarcodeEvent {
    data object PrintSuccess : PrintBarcodeEvent()
    data class ShowError(val message: String) : PrintBarcodeEvent()
}
