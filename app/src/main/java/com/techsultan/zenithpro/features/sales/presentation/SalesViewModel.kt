package com.techsultan.zenithpro.features.sales.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.manager.ReceiptNumberGenerator
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.branch.domain.use_case.GetBranchesUseCase
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.SaleStatus
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import com.techsultan.zenithpro.features.sales.data.remote.CartItem
import com.techsultan.zenithpro.features.sales.data.remote.CompletedSale
import com.techsultan.zenithpro.features.sales.data.remote.SaleFilter
import com.techsultan.zenithpro.features.sales.domain.repository.SaleRepository
import com.techsultan.zenithpro.features.sales.domain.use_case.GenerateReceiptUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.GetSalesUseCase
import com.techsultan.zenithpro.features.settings.data.remote.StaffMember
import com.techsultan.zenithpro.features.settings.domain.use_case.GetSettingsUseCase
import com.techsultan.zenithpro.features.settings.domain.use_case.GetStaffListUseCase
import kotlinx.coroutines.Job
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SalesListViewModel(
    private val getSalesUseCase: GetSalesUseCase,
    private val saleRepository: SaleRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager,
    private val generateReceiptUseCase: GenerateReceiptUseCase,
    private val receiptNumberGenerator: ReceiptNumberGenerator,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val getBranchesUseCase: GetBranchesUseCase,
    private val getStaffListUseCase: GetStaffListUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SalesListUiState())
    val state: StateFlow<SalesListUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SalesListEvent>()
    val events = _events.asSharedFlow()

    private val currentSession get() = sessionManager.currentSession
    private var businessId: String? = null
    private var observeJob: Job? = null
    private var footerMessage: String? = ""
    private var taxRate: Double = 0.0

    init {
        viewModelScope.launch {
            val session = sessionManager.loadSession()
            businessId = session?.businessId
            if (session != null) {

                if (session.hasBranch && !session.isManager) {
                    _state.update { it.copy(filterBranchId = session.branchId) }
                }

                observeSales()
                syncOnStart()
                loadBranches()
                loadStaff()
                getSettingsUseCase(session.businessId).collect { settings ->
                    footerMessage = settings?.receiptFooter
                    taxRate = settings?.taxRate ?: 0.0
                }
            }
        }
    }

    private fun observeSales() {
        val bId = businessId ?: return
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            getSalesUseCase(bId, buildFilter()).collect { result ->
                when (result) {
                    is Resource.Loading -> _state.update {
                        it.copy(isLoading = it.sales.isEmpty())
                    }
                    is Resource.Success -> _state.update {
                        Log.d("SalesListViewModel", "Sales Result: ${result.data?.size}")
                        it.copy(
                            isLoading = false,
                            sales     = result.data ?: emptyList(),
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

    private fun loadBranches() {
        viewModelScope.launch {
            val bId = businessId ?: return@launch
            getBranchesUseCase.invoke(bId).collect { result ->
                when(result){
                    is Resource.Error -> {}
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        _state.update { it.copy(availableBranches = result.data ?: emptyList()) }
                    }
                }
            }
        }
    }

    private fun loadStaff() {
        viewModelScope.launch {
            val bId = businessId ?: return@launch
            when (val result = getStaffListUseCase(bId)) {
                is Resource.Success -> {
                    _state.update { it.copy(availableStaff = result.data ?: emptyList()) }
                }
                else -> {}
            }
        }
    }

    fun onDateRangeChanged(from: LocalDate, to: LocalDate) {
        _state.update { it.copy(filterFrom = from, filterTo = to) }
        restartObserver()
    }

    fun onStaffFilterChanged(staffId: String?) {
        _state.update { it.copy(filterStaffId = staffId) }
        restartObserver()
    }

    fun onPaymentMethodFilterChanged(method: PaymentMethod?) {
        _state.update { it.copy(filterPaymentMethod = method) }
        restartObserver()
    }

    fun clearFilters() {
        _state.update { it.copy(
            filterFrom  = LocalDate.now().minusDays(30),
            filterTo = LocalDate.now(),
            filterStaffId = null,
            filterPaymentMethod = null,
            filterBranchId = null
        )}
        restartObserver()
    }

    fun onBranchFilterChanged(branchId: String?) {
        _state.update { it.copy(filterBranchId = branchId) }
        restartObserver()
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun refresh() {
        val bId = businessId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            saleRepository.pullSalesFromServer(bId)
            _state.update { it.copy(isRefreshing = false) }
        }
    }
    
    fun reprintReceipt(saleWithItems: SaleWithItems) {
        viewModelScope.launch {
            val receipt = prepareReceiptData(saleWithItems)
            _events.emit(SalesListEvent.ReceiptReady(receipt))
        }
    }

    fun onShareSale(saleWithItems: SaleWithItems) {
        viewModelScope.launch {
            val receipt = prepareReceiptData(saleWithItems)
            _events.emit(SalesListEvent.ReceiptReady(receipt))
        }
    }

    private suspend fun prepareReceiptData(saleWithItems: SaleWithItems): ReceiptData {
        val receiptNumber = receiptNumberGenerator.generate()
        val sale = saleWithItems.sale
        val items = saleWithItems.items.map {
            CartItem(
                variantId = it.variantId,
                productId = it.productId,
                productName = it.productName,
                variantSku = it.variantSku,
                unitPrice = it.unitPrice,
                costPrice = it.costPrice,
                quantity = it.quantity
            )
        }

        val session = currentSession
        val branch = state.value.availableBranches.find { it.id == sale.branchId }

        val completedSale = CompletedSale(
            saleId = sale.id,
            receiptNumber = receiptNumber,
            salesPerson = session?.firstName ?: "Staff",
            paymentMethod = sale.paymentMethod.name,
            subtotal = sale.totalAmount + sale.discountAmount,
            discount = sale.discountAmount,
            total = sale.totalAmount,
            amountPaid = sale.amountPaid,
            change = maxOf(0L, sale.amountPaid - sale.totalAmount),
            cartItems = items,
            customer = null,
            splitPayments = emptyList(),
            createdAt = try {
                Instant.parse(sale.soldAt).toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            },
            businessName = branch?.name ?: session?.businessName ?: "",
            businessAddress = branch?.address ?: session?.businessAddress ?: "",
            businessNumber = branch?.phone ?: session?.businessPhone ?: "",
            taxRate = taxRate,
            footerMessage = footerMessage
        )

        return generateReceiptUseCase(completedSale)
    }

    val filteredSales: StateFlow<List<SaleWithItems>> = state
        .map { s ->
            Log.d("SalesListViewModel", "Filtered sales: ${s.sales.size}")
            if (s.searchQuery.isBlank()) s.sales
            else s.sales.filter { saleWithItems ->
                val sale = saleWithItems.sale
                saleWithItems.items.any { item ->
                    item.productName.contains(s.searchQuery, ignoreCase = true) ||
                            item.variantSku.contains(s.searchQuery, ignoreCase = true)
                } || sale.notes?.contains(s.searchQuery, ignoreCase = true) == true
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summaryStats: StateFlow<SalesSummary> = state
        .map { s ->
            val sales = s.sales.filter {
                it.sale.status != SaleStatus.CANCELLED
            }
            SalesSummary(
                totalRevenue     = sales.sumOf { it.sale.totalAmount },
                cashRevenue      = sales.filter { it.sale.paymentMethod == PaymentMethod.CASH }.sumOf { it.sale.totalAmount },
                transferRevenue  = sales.filter { it.sale.paymentMethod == PaymentMethod.TRANSFER }.sumOf { it.sale.totalAmount },
                cardRevenue      = sales.filter { 
                    it.sale.paymentMethod == PaymentMethod.POS || 
                    it.sale.paymentMethod == PaymentMethod.CARD ||
                    it.sale.paymentMethod == PaymentMethod.USSD
                }.sumOf { it.sale.totalAmount },
                totalCollected   = sales.sumOf { it.sale.amountPaid },
                totalDebt        = sales.sumOf { it.sale.debtAmount },
                totalOrders      = sales.size,
                averageOrderValue = if (sales.isEmpty()) 0L
                else sales.sumOf { it.sale.totalAmount } / sales.size
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SalesSummary())


    private fun syncOnStart() {
        val bId = businessId ?: return
        viewModelScope.launch {
            if (networkMonitor.isConnected()) {
                saleRepository.pullSalesFromServer(bId)
            }
        }
    }

    private fun restartObserver() {
        observeSales()
    }

    private fun buildFilter(): SaleFilter {
        val s = _state.value
        return SaleFilter(
            from          = s.filterFrom.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toString(),
            to            = s.filterTo.plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toString(),
            staffId       = s.filterStaffId,
            paymentMethod = s.filterPaymentMethod?.name,
            branchId      = s.filterBranchId
        )
    }

    sealed class SalesListEvent {
        data class ShowError(val message: String) : SalesListEvent()
        data object PrintSuccess : SalesListEvent()
        data class ReceiptReady(val receipt: ReceiptData) : SalesListEvent()
    }
}

data class SalesListUiState(
    val isLoading: Boolean  = false,
    val isRefreshing: Boolean  = false,
    val sales: List<SaleWithItems> = emptyList(),
    val searchQuery: String  = "",
    val filterFrom: LocalDate = LocalDate.now().minusDays(30),
    val filterTo: LocalDate = LocalDate.now(),
    val filterStaffId: String? = null,
    val filterPaymentMethod: PaymentMethod? = null,
    val filterBranchId: String? = null,
    val availableBranches: List<BranchEntity> = emptyList(),
    val availableStaff: List<StaffMember> = emptyList(),
    val error: String? = null
)

data class SalesSummary(
    val totalRevenue: Long      = 0L,
    val cashRevenue: Long       = 0L,
    val transferRevenue: Long   = 0L,
    val cardRevenue: Long       = 0L,
    val totalCollected: Long    = 0L,
    val totalDebt: Long         = 0L,
    val totalOrders: Int        = 0,
    val averageOrderValue: Long = 0L,
)
