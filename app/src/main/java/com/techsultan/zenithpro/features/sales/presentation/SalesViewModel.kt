package com.techsultan.zenithpro.features.sales.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.SaleStatus
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import com.techsultan.zenithpro.features.sales.data.remote.SaleFilter
import com.techsultan.zenithpro.features.sales.domain.repository.SaleRepository
import com.techsultan.zenithpro.features.sales.domain.use_case.GetSalesUseCase
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
import java.time.LocalDate
import java.time.ZoneId

class SalesListViewModel(
    private val getSalesUseCase: GetSalesUseCase,
    private val saleRepository: SaleRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(SalesListUiState())
    val state: StateFlow<SalesListUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SalesListEvent>()
    val events = _events.asSharedFlow()

    private var businessId: String = ""
    private var observeJob: Job? = null

    fun init(businessId: String) {
        if (this.businessId == businessId) return
        this.businessId = businessId
        observeSales()
        syncOnStart()
    }

    private fun observeSales() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            getSalesUseCase(businessId, buildFilter()).collect { result ->
                when (result) {
                    is Resource.Loading -> _state.update {
                        it.copy(isLoading = it.sales.isEmpty())
                    }
                    is Resource.Success -> _state.update {
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

    // ── Filters ────────────────────────────────────────────────────

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
            filterFrom          = LocalDate.now().minusDays(30),
            filterTo            = LocalDate.now(),
            filterStaffId       = null,
            filterPaymentMethod = null
        )}
        restartObserver()
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    // ── Refresh ────────────────────────────────────────────────────

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            saleRepository.pullSalesFromServer(businessId)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    // ── Derived data — computed from state, no extra DB queries ───

    val filteredSales: StateFlow<List<SaleWithItems>> = state
        .map { s ->
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
                totalCollected   = sales.sumOf { it.sale.amountPaid },
                totalDebt        = sales.sumOf { it.sale.debtAmount },
                totalOrders      = sales.size,
                averageOrderValue = if (sales.isEmpty()) 0L
                else sales.sumOf { it.sale.totalAmount } / sales.size
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SalesSummary())

    // ── Private helpers ────────────────────────────────────────────

    private fun syncOnStart() {
        viewModelScope.launch {
            if (networkMonitor.isConnected()) {
                saleRepository.pullSalesFromServer(businessId)
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
            paymentMethod = s.filterPaymentMethod?.name
        )
    }

    sealed class SalesListEvent {
        data class ShowError(val message: String) : SalesListEvent()
    }
}

data class SalesListUiState(
    val isLoading: Boolean              = false,
    val isRefreshing: Boolean           = false,
    val sales: List<SaleWithItems>      = emptyList(),
    val searchQuery: String             = "",
    val filterFrom: LocalDate = LocalDate.now().minusDays(30),
    val filterTo: LocalDate             = LocalDate.now(),
    val filterStaffId: String?          = null,
    val filterPaymentMethod: PaymentMethod? = null,
    val error: String?                  = null,
   // val availableStaff: List<Staff> = emptyList()
)

data class SalesSummary(
    val totalRevenue: Long      = 0L,
    val cashRevenue: Long       = 0L,
    val transferRevenue: Long   = 0L,
    val cardRevenue: Long       = 0L,
    val totalCollected: Long    = 0L,
    val totalDebt: Long         = 0L,
    val totalOrders: Int        = 0,
    val averageOrderValue: Long = 0L
)