package com.techsultan.zenithpro.features.sales.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.data.local.SplitPayment
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.domain.repository.CustomerRepository
import com.techsultan.zenithpro.features.customer.domain.use_case.GetCustomerDetailUseCase
import com.techsultan.zenithpro.features.product.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.product.domain.use_case.GetProductsUseCase
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.data.remote.CartItem
import com.techsultan.zenithpro.features.sales.data.remote.CompletedSale
import com.techsultan.zenithpro.features.sales.domain.repository.PrinterRepository
import com.techsultan.zenithpro.features.sales.domain.use_case.GenerateReceiptUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.GetDailySummaryUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.GetSalesUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.ProcessSaleUseCase
import com.techsultan.zenithpro.features.sales.generateReceiptNumber
import com.techsultan.zenithpro.features.sales.data.remote.CartItem as RemoteCartItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CheckoutViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val processSaleUseCase: ProcessSaleUseCase,
    private val getSaleUseCase: GetSalesUseCase,
    private val getDailySummaryUseCase: GetDailySummaryUseCase,
    private val sessionManager: SessionManager,
    private val customerRepository: CustomerRepository,
    private val getCustomerDetailUseCase: GetCustomerDetailUseCase,
    private val generateReceiptUseCase: GenerateReceiptUseCase,
    private val printerRepository: PrinterRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NewSaleUiState())
    val state: StateFlow<NewSaleUiState> = _state.asStateFlow()

    private val _newlyCreatedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val newlyCreatedCustomer: StateFlow<CustomerEntity?> = _newlyCreatedCustomer.asStateFlow()

    private val _events = MutableSharedFlow<CheckoutEvent>()
    val events = _events.asSharedFlow()

    val currentSession = sessionManager.currentSession
    private var businessId: String? = null

    private val _cart = MutableStateFlow<Map<String, RemoteCartItem>>(emptyMap())
    val cart: StateFlow<Map<String, RemoteCartItem>> = _cart.asStateFlow()

    val cartTotal: StateFlow<Long> = cart.map { it.values.sumOf { item -> item.unitPrice * item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val cartItemCount: StateFlow<Int> = cart.map { it.values.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Customer search
    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery: StateFlow<String> = _customerSearchQuery.asStateFlow()

    val customerSearchResults: StateFlow<List<CustomerEntity>> =
        _customerSearchQuery
            .debounce(300)
            .flatMapLatest { query ->
                if (query.isBlank()) flowOf(emptyList())
                else customerRepository.searchCustomers(
                    businessId ?: return@flatMapLatest flowOf(emptyList()),
                    query
                ).map { result ->
                    (result as? Resource.Success)?.data ?: emptyList()
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Split payment fields
    private val _splitCashAmount = MutableStateFlow(0L)
    val splitCashAmount: StateFlow<Long> = _splitCashAmount.asStateFlow()

    private val _splitTransferAmount = MutableStateFlow(0L)
    val splitTransferAmount: StateFlow<Long> = _splitTransferAmount.asStateFlow()

    init {
        viewModelScope.launch {
            businessId = currentSession?.businessId
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
                    variantSku = variant.variant.sku ?: "",
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
        // Reset split amounts when switching methods
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

        // Validate based on payment method
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

            val remoteCart = currentCartMap.values.map {
                RemoteCartItem(
                    variantId = it.variantId,
                    productId = it.productId,
                    productName = it.productName,
                    variantSku = it.variantSku,
                    unitPrice = it.unitPrice,
                    costPrice = it.costPrice,
                    quantity = it.quantity
                )
            }

            val branchId = currentSession?.branchId
            val subtotal = remoteCart.sumOf { it.totalPrice }
            val total = maxOf(0L, subtotal - s.discountAmount)

            // Calculate effective paid amount based on payment method
            val effectivePaid = when (s.paymentMethod) {
                PaymentMethod.CASH,
                PaymentMethod.CARD,
                PaymentMethod.TRANSFER -> if (s.amountPaid <= 0L) total else s.amountPaid
                PaymentMethod.DEBT -> 0L
                PaymentMethod.SPLIT -> _splitCashAmount.value + _splitTransferAmount.value
                PaymentMethod.POS,
                PaymentMethod.USSD -> if (s.amountPaid <= 0L) total else s.amountPaid
            }

            val result = processSaleUseCase(
                cart = remoteCart,
                customerId = s.selectedCustomerId,
                branchId = branchId,
                amountPaid = effectivePaid,
                paymentMethod = s.paymentMethod,
                discountAmount = s.discountAmount,
                notes = notes,
                staffId = sessionManager.userId
            )

            when (result) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _events.emit(CheckoutEvent.SaleCompleted(
                        saleId = result.data?.saleId ?: "",
                        change = maxOf(0L, effectivePaid - total),
                        debtAmount = result.data?.debtAmount ?: 0L
                    ))
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
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
            val businessId = currentSession?.businessId ?: return@launch
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
        val subtotal = currentCartList.sumOf { it.unitPrice * it.quantity }
        val total = maxOf(0L, subtotal - s.discountAmount)

        val completedSale = CompletedSale(
            saleId = saleId,
            salesPerson = "${currentSession?.firstName} ${currentSession?.lastName ?: ""}".trim(),
            paymentMethod = s.paymentMethod.name,
            subtotal = subtotal,
            discount = s.discountAmount,
            total = total,
            amountPaid = amountPaid,
            change = change,
            cartItems = currentCartList,
            customer = s.selectedCustomer,
            splitPayments = buildSplitPayments(),
            createdAt = System.currentTimeMillis()
        )

        val receipt = generateReceiptUseCase(completedSale)

        _events.emit(CheckoutEvent.PaymentCompleted(receipt))

        val printResult = printerRepository.printReceipt(receipt)
        if (printResult is Resource.Error) {
            _events.emit(CheckoutEvent.PrintFailed(printResult.message ?: "Printing failed"))
        }
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
        data class SaleCompleted(
            val saleId: String,
            val change: Long,
            val debtAmount: Long
        ) : CheckoutEvent()

        data class PaymentCompleted(
            val receipt: ReceiptData
        ) : CheckoutEvent()

        data class PrintFailed(
            val message: String
        ) : CheckoutEvent()
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
    val showCustomerSelector: Boolean = false
)
