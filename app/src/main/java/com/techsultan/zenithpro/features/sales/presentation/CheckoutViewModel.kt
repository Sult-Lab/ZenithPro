package com.techsultan.zenithpro.features.sales.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.data.local.SplitPayment
import com.techsultan.zenithpro.core.manager.ReceiptNumberGenerator
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.ZenithAnalytics
import androidx.core.os.bundleOf
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.domain.use_case.GetCustomerDetailUseCase
import com.techsultan.zenithpro.features.customer.domain.repository.CustomerRepository
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.inventory.domain.use_case.GetProductsUseCase
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.data.remote.CartItem
import com.techsultan.zenithpro.features.sales.data.remote.CompletedSale
import com.techsultan.zenithpro.features.sales.domain.use_case.GenerateReceiptUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.ProcessSaleUseCase
import com.techsultan.zenithpro.features.settings.data.local.TerminalEntity
import com.techsultan.zenithpro.features.settings.domain.use_case.GetSettingsUseCase
import com.techsultan.zenithpro.features.settings.domain.use_case.GetTerminalsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CheckoutViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val processSaleUseCase: ProcessSaleUseCase,
    private val sessionManager: SessionManager,
    private val customerRepository: CustomerRepository,
    private val getCustomerDetailUseCase: GetCustomerDetailUseCase,
    private val generateReceiptUseCase: GenerateReceiptUseCase,
    private val receiptNumberGenerator: ReceiptNumberGenerator,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val getTerminalsUseCase: GetTerminalsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NewSaleUiState())
    val state: StateFlow<NewSaleUiState> = _state.asStateFlow()

    private val _cart = MutableStateFlow<Map<String, CartItem>>(emptyMap())
    val cart: StateFlow<Map<String, CartItem>> = _cart.asStateFlow()

    private val _events = MutableSharedFlow<CheckoutEvent>()
    val events = _events.asSharedFlow()

    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery = _customerSearchQuery.asStateFlow()

    private val _splitCashAmount = MutableStateFlow(0L)
    val splitCashAmount = _splitCashAmount.asStateFlow()

    private val _splitTransferAmount = MutableStateFlow(0L)
    val splitTransferAmount = _splitTransferAmount.asStateFlow()

    private val _newlyCreatedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val newlyCreatedCustomer = _newlyCreatedCustomer.asStateFlow()

    private val _currentTerminal = MutableStateFlow<TerminalEntity?>(null)
    val currentTerminal = _currentTerminal.asStateFlow()

    private val _paymentStatus = MutableStateFlow<String?>(null)
    val paymentStatus = _paymentStatus.asStateFlow()

    val cartTotal: StateFlow<Long> = _cart.map { it.values.sumOf { item -> item.totalPrice } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val cartItemCount: StateFlow<Double> = _cart.map { it.values.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val customerSearchResults: StateFlow<List<CustomerEntity>> = combine(
        _customerSearchQuery,
        sessionManager.sessionFlow
    ) { query, session ->
        if (query.isBlank() || session == null) emptyList()
        else {
            customerRepository.searchCustomers(session.businessId, query)
                .map { it.data ?: emptyList() }
                .first()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val currentSession get() = sessionManager.currentSession

    init {
        viewModelScope.launch {
            sessionManager.loadSession()
            
            getSettingsUseCase(currentSession?.businessId ?: "").collect { settings ->
                _state.update { it.copy(
                    footerMessage = settings?.receiptFooter ?: "",
                    taxRate = settings?.taxRate ?: 0.0
                ) }
            }
        }

        viewModelScope.launch {
            sessionManager.activeBranchId.collect { id ->
                _state.update { it.copy(
                    selectedBranchId = id,
                    selectedBranchName = sessionManager.activeBranchName.value
                ) }
                loadTerminalForCurrentBranch()
                loadProducts(id) 
            }
        }
    }

    fun loadTerminalForCurrentBranch() {
        viewModelScope.launch {
            val session = sessionManager.currentSession ?: return@launch
            val branchId = sessionManager.activeBranchId.value

            getTerminalsUseCase(sessionManager.businessId).collect { result ->
                if (result is Resource.Success) {
                    val terminals = result.data ?: emptyList()
                    _currentTerminal.value = when {
                        branchId == null -> terminals.firstOrNull { it.isActive }
                        else -> terminals.firstOrNull {
                            it.branchId == branchId && it.isActive
                        }
                    }
                    Log.d("CheckoutVM",
                        "Terminal resolved: ${_currentTerminal.value?.id} " +
                                "branchId=$branchId isAdmin=${session.isAdmin}"
                    )
                }
            }
        }
    }

    private fun loadProducts(branchId: String? = null) {
        viewModelScope.launch {
            val businessId = currentSession?.businessId
            businessId?.let { 
                if (branchId != null) getProductsUseCase(it, branchId) 
                else getProductsUseCase(it)
            }?.collect { result ->
                when (result) {
                    is Resource.Loading -> _state.update { it.copy(isLoading = true) }
                    is Resource.Success -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                products = result.data ?: emptyList(),
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                        _events.emit(CheckoutEvent.ShowError(result.message ?: "An error occurred"))
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
            val branchId = sessionManager.activeBranchId.value
            loadProducts(branchId)
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
                    productName = product.product.name,
                    unitPrice = product.product.baseSalesPrice,
                    costPrice = product.product.baseCostPrice,
                    variantSku = variant.variant.sku,
                    unitType = product.product.unitType,
                    quantity = 1.0
                )
            }
            ZenithAnalytics.trackEvent("cart_item_added", bundleOf(
                "product_category" to (product.product.category ?: "none"),
                "unit_type" to product.product.unitType
            ))
            updatedCart
        }
    }

    fun onBarcodeScanned(barcode: String): Boolean {
        val product = state.value.products.find { p ->
            p.variants.any { v -> v.variant.barcode == barcode }
        }
        return if (product != null) {
            addToCart(product)
            viewModelScope.launch {
                _events.emit(CheckoutEvent.ProductAddedByBarcode(product.product.name))
            }
            true
        } else {
            viewModelScope.launch {
                _events.emit(CheckoutEvent.ShowError("Product not found for barcode: $barcode"))
            }
            false
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
            ZenithAnalytics.trackEvent("cart_item_removed")
            updatedCart
        }
    }

    fun updateCartItemQuantity(variantId: String, quantity: Double) {
        _cart.update { currentCart ->
            val updatedCart = currentCart.toMutableMap()
            val entry = updatedCart.entries.find { it.value.variantId == variantId }
            if (entry != null) {
                if (quantity > 0) {
                    updatedCart[entry.key] = entry.value.copy(quantity = quantity)
                } else {
                    updatedCart.remove(entry.key)
                }
            }
            updatedCart
        }
    }

    fun setDiscount(amount: Long) {
        _state.update { it.copy(discountAmount = amount) }
    }

    fun setCustomer(customerId: String?, customer: CustomerEntity? = null) {
        _state.update { it.copy(selectedCustomerId = customerId, selectedCustomer = customer) }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _state.update {
            it.copy(
                paymentMethod = method,
                amountPaid = 0L,
                showCustomerSelector = method in listOf(PaymentMethod.DEBT, PaymentMethod.SPLIT)
            )
        }
        if (method != PaymentMethod.SPLIT) {
            _splitCashAmount.value = 0L
            _splitTransferAmount.value = 0L
        }
    }

    fun setAmountPaid(amount: Long) {
        _state.update { it.copy(amountPaid = amount) }
    }

    fun setSplitAmounts(cashAmount: Long, transferAmount: Long) {
        _splitCashAmount.value = cashAmount
        _splitTransferAmount.value = transferAmount
        _state.update { it.copy(amountPaid = cashAmount + transferAmount) }
    }

    fun onCustomerSearchChanged(query: String) {
        _customerSearchQuery.value = query
    }

    fun clearCustomer() {
        _state.update {
            it.copy(selectedCustomerId = null, selectedCustomer = null, showCustomerSelector = false)
        }
        _customerSearchQuery.value = ""
    }

    fun confirmCustomerSelection() {
        _state.update { it.copy(showCustomerSelector = false) }
    }

    fun clearCart() {
        if (_cart.value.isNotEmpty()) {
            ZenithAnalytics.trackEvent("checkout_abandoned", bundleOf("step" to "cart"))
        }
        _cart.value = emptyMap()
        _state.update { it.copy(
            discountAmount = 0L,
            amountPaid = 0L,
            selectedCustomerId = null,
            selectedCustomer = null,
            paymentMethod = PaymentMethod.CASH,
            showCustomerSelector = false
        ) }
        _splitCashAmount.value = 0L
        _splitTransferAmount.value = 0L
        _customerSearchQuery.value = ""
    }

    fun checkout(notes: String? = null) {
        val currentCartMap = _cart.value
        if (currentCartMap.isEmpty()) return

        val s = _state.value
        if (s.isLoading) return
        Log.d("CheckoutVM", "Terminal ID: ${_currentTerminal.value}")

        ZenithAnalytics.trackEvent("checkout_started", bundleOf(
            "item_count" to cartItemCount.value,
            "cart_value_kobo" to cartTotal.value
        ))

        when (s.paymentMethod) {
            PaymentMethod.DEBT -> {
                if (s.selectedCustomerId == null) {
                    viewModelScope.launch {
                        _events.emit(CheckoutEvent.ShowError("Customer required for debt payment"))
                    }
                    return
                }
            }
            PaymentMethod.SPLIT -> {
                if (s.selectedCustomerId == null) {
                    viewModelScope.launch {
                        _events.emit(CheckoutEvent.ShowError("Customer required for split payment"))
                    }
                    return
                }
                val totalPaid = _splitCashAmount.value + _splitTransferAmount.value
                val totalAmount = cartTotal.value - s.discountAmount
                if (totalPaid < totalAmount) {
                    viewModelScope.launch {
                        _events.emit(CheckoutEvent.ShowError("Split amounts must cover the total"))
                    }
                    return
                }
            }
            else -> {}
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val cartItemsList = currentCartMap.values.toList()

            val subtotal = cartItemsList.sumOf { it.totalPrice }
            val total = maxOf(0L, subtotal - s.discountAmount)
            val taxAmount = (total * (s.taxRate / 100)).toLong()
            val effectivePaid = when (s.paymentMethod) {
                PaymentMethod.CASH,
                PaymentMethod.CARD,
                PaymentMethod.TRANSFER -> if (s.amountPaid <= 0L) total else s.amountPaid
                PaymentMethod.DEBT -> 0L
                PaymentMethod.SPLIT -> _splitCashAmount.value + _splitTransferAmount.value
                PaymentMethod.POS,
                PaymentMethod.USSD -> if (s.amountPaid <= 0L) total else s.amountPaid
            }

            if (s.selectedBranchId == null) return@launch
            val result = processSaleUseCase(
                cart = cartItemsList,
                customerId = s.selectedCustomerId,
                branchId = s.selectedBranchId,
                amountPaid = effectivePaid,
                paymentMethod = s.paymentMethod,
                discountAmount = s.discountAmount,
                notes = notes,
                staffId = sessionManager.userId,
                taxAmount = taxAmount,
            )

            when (result) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    val response = result.data
                    val saleId = response?.saleId ?: ""
                    val change = maxOf(0L, effectivePaid - total)

                    _events.emit(CheckoutEvent.SaleCompleted(
                        saleId = saleId,
                        change = change,
                        debtAmount = result.data?.debtAmount ?: 0L,
                        customer = s.selectedCustomer?.firstName ?: ""
                    ))

                    completePayment(saleId, effectivePaid, change)
                    clearCart()
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                    ZenithAnalytics.logError(Exception(result.message), context = "CheckoutViewModel.checkout")
                    _events.emit(CheckoutEvent.ShowError(result.message ?: "Sale failed"))
                }
                else -> {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onCustomerCreated(customerId: String) {
        viewModelScope.launch {
            getCustomerDetailUseCase(customerId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { customer ->
                            _newlyCreatedCustomer.value = customer
                            setCustomer(customer.id, customer)
                        }
                    }
                    is Resource.Error -> {
                        _events.emit(CheckoutEvent.ShowError("Failed to load customer: ${result.message}"))
                    }
                    else -> Unit
                }
            }
        }
    }

    fun clearNewlyCreatedCustomer() {
        _newlyCreatedCustomer.value = null
    }

    private suspend fun completePayment(
        saleId: String,
        amountPaid: Long,
        change: Long
    ) {
        val s = state.value
        val currentCartList = _cart.value.values.toList()
        val subtotal = currentCartList.sumOf { it.unitPrice * it.quantity }.toLong()
        val total = maxOf(0L, subtotal - s.discountAmount)
        val receiptNumber = receiptNumberGenerator.generate()

        val completedSale = CompletedSale(
            saleId = saleId,
            receiptNumber = receiptNumber,
            salesPerson = currentSession?.fullName ?: "Staff",
            paymentMethod = s.paymentMethod.name,
            subtotal = subtotal,
            discount = s.discountAmount,
            total = total,
            amountPaid = amountPaid,
            change = change,
            cartItems = currentCartList,
            customer = s.selectedCustomer,
            splitPayments = buildSplitPayments(),
            createdAt = System.currentTimeMillis(),
            businessName = currentSession?.businessName ?: "",
            businessAddress = currentSession?.businessAddress ?: "",
            businessNumber = currentSession?.businessName ?: "",
            taxRate = s.taxRate,
            footerMessage = s.footerMessage
        )

        val receipt = generateReceiptUseCase(completedSale)

        _events.emit(CheckoutEvent.ReceiptReady(receipt))

    }

    private fun buildSplitPayments(): List<SplitPayment> {
        return if (state.value.paymentMethod == PaymentMethod.SPLIT) {
            val list = mutableListOf<SplitPayment>()
            if (_splitCashAmount.value > 0) {
                list.add(SplitPayment("CASH", _splitCashAmount.value))
            }
            if (_splitTransferAmount.value > 0) {
                list.add(SplitPayment("TRANSFER", _splitTransferAmount.value))
            }
            list
        } else emptyList()
    }

    sealed class CheckoutEvent {
        data class ShowError(val message: String) : CheckoutEvent()

        data class ReceiptReady(
            val receipt: ReceiptData
        ) : CheckoutEvent()

        data class SaleCompleted(
            val saleId: String,
            val change: Long,
            val debtAmount: Long,
            val customer: String
        ) : CheckoutEvent()

        data class ProductAddedByBarcode(val productName: String) : CheckoutEvent()
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
    val selectedCustomer: CustomerEntity? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val showCustomerSelector: Boolean = false,
    val footerMessage: String? = null,
    val taxRate: Double = 0.0,

    val selectedBranchId: String?         = null,
    val selectedBranchName: String?       = null,
)
