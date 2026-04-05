package com.techsultan.zenithpro.features.sales.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.product.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.product.domain.use_case.GetProductsUseCase
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.domain.use_case.GetDailySummaryUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.GetSalesUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.ProcessSaleUseCase
import com.techsultan.zenithpro.features.sales.data.remote.CartItem as RemoteCartItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewSaleViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val processSaleUseCase: ProcessSaleUseCase,
    private val getSaleUseCase: GetSalesUseCase,
    private val getDailySummaryUseCase: GetDailySummaryUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(NewSaleUiState())
    val state: StateFlow<NewSaleUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<NewSaleEvent>()
    val events = _events.asSharedFlow()

    private var businessId: String? = null

    private val _cart = MutableStateFlow<Map<String, CartItem>>(emptyMap())
    val cart: StateFlow<Map<String, CartItem>> = _cart.asStateFlow()

    val cartTotal: StateFlow<Long> = cart.map { it.values.sumOf { item -> item.price * item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val cartItemCount: StateFlow<Int> = cart.map { it.values.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            businessId = sessionManager.loadSession()?.businessId
            if (businessId != null) {
                observeProducts()
            }
        }
    }

    private fun observeProducts() {
        val bId = businessId ?: return
        viewModelScope.launch {
            getProductsUseCase(bId).collect { result ->
                when (result) {
                    is Resource.Loading -> _state.update { it.copy(isLoading = it.products.isEmpty()) }
                    is Resource.Success -> _state.update {
                        it.copy(
                            isLoading = false,
                            products = result.data ?: emptyList(),
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                        _events.emit(NewSaleEvent.ShowError(result.message ?: "An error occurred"))
                    }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun onCategoryFilterChanged(category: String?) {
        _state.update { it.copy(selectedCategory = category) }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    val filteredProducts: StateFlow<List<ProductWithVariants>> =
        state.map { s ->
            s.products.filter { p ->
                val matchesSearch = s.searchQuery.isBlank() ||
                        p.product.name.contains(s.searchQuery, ignoreCase = true) ||
                        p.product.category?.contains(s.searchQuery, ignoreCase = true) == true

                val matchesCategory = s.selectedCategory == null || s.selectedCategory == "All" ||
                        p.product.category == s.selectedCategory

                matchesSearch && matchesCategory
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = state.map { s ->
        listOf("All") + s.products.mapNotNull { it.product.category }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    fun addToCart(product: ProductWithVariants) {
        _cart.update { currentCart ->
            val existingItem = currentCart[product.product.id]
            val updatedCart = currentCart.toMutableMap()
            if (existingItem != null) {
                updatedCart[product.product.id] = existingItem.copy(quantity = existingItem.quantity + 1)
            } else {
                val variant = product.variants.firstOrNull() ?: return@update currentCart
                updatedCart[product.product.id] = CartItem(
                    productId = product.product.id,
                    variantId = variant.variant.id,
                    name = product.product.name,
                    price = product.product.baseSalesPrice,
                    costPrice = product.product.baseCostPrice,
                    sku = variant.variant.sku ?: "",
                    quantity = 1
                )
            }
            updatedCart
        }
    }

    fun removeFromCart(productId: String) {
        _cart.update { currentCart ->
            val existingItem = currentCart[productId] ?: return@update currentCart
            val updatedCart = currentCart.toMutableMap()
            if (existingItem.quantity > 1) {
                updatedCart[productId] = existingItem.copy(quantity = existingItem.quantity - 1)
            } else {
                updatedCart.remove(productId)
            }
            updatedCart
        }
    }

    fun checkout(branchId: String? = null, notes: String? = null) {
        val currentCartMap = _cart.value
        if (currentCartMap.isEmpty()) return
        
        val s = _state.value
        if (s.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val remoteCart = currentCartMap.values.map { 
                RemoteCartItem(
                    variantId = it.variantId,
                    productId = it.productId,
                    productName = it.name,
                    variantSku = it.sku,
                    unitPrice = it.price,
                    costPrice = it.costPrice,
                    quantity = it.quantity
                )
            }

            val result = processSaleUseCase(
                cart            = remoteCart,
                customerId      = s.selectedCustomerId,
                branchId        = branchId,
                amountPaid      = s.amountPaid,
                paymentMethod   = s.paymentMethod,
                discountAmount  = s.discountAmount,
                notes           = notes
            )

            when (result) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _events.emit(NewSaleEvent.SaleCompleted(
                        saleId      = result.data?.saleId ?: "",
                        change      = maxOf(0L, s.amountPaid - (remoteCart.sumOf { it.totalPrice } - s.discountAmount)),
                        debtAmount  = result.data?.debtAmount ?: 0L
                    ))
                    clearCart()
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                    _events.emit(NewSaleEvent.ShowError(result.message ?: "Sale failed"))
                }
                else -> {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun setDiscount(amount: Long) {
        _state.update { it.copy(discountAmount = amount) }
    }

    fun setCustomer(customerId: String?) {
        _state.update { it.copy(selectedCustomerId = customerId) }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _state.update { it.copy(paymentMethod = method) }
    }

    fun setAmountPaid(amount: Long) {
        _state.update { it.copy(amountPaid = amount) }
    }

    fun clearCart() {
        _cart.value = emptyMap()
        _state.update { it.copy(
            discountAmount = 0L,
            amountPaid = 0L,
            selectedCustomerId = null,
            paymentMethod = PaymentMethod.CASH
        ) }
    }

    sealed class NewSaleEvent {
        data class ShowError(val message: String) : NewSaleEvent()
        data class SaleCompleted(
            val saleId: String,
            val change: Long,
            val debtAmount: Long
        ) : NewSaleEvent()
    }
}

data class NewSaleUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val products: List<ProductWithVariants> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = "All",
    val error: String? = null,
    
    // Checkout related
    val discountAmount: Long = 0L,
    val amountPaid: Long = 0L,
    val selectedCustomerId: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH
)

data class CartItem(
    val productId: String,
    val variantId: String,
    val name: String,
    val price: Long,
    val costPrice: Long,
    val sku: String,
    val quantity: Int
)
