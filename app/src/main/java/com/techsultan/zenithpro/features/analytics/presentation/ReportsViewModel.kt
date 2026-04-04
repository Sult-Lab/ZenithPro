package com.techsultan.zenithpro.features.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.features.customer.data.local.CustomerDao
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.data.remote.CustomerStats
import com.techsultan.zenithpro.features.dashboard.data.remote.ChartDataPoint
import com.techsultan.zenithpro.features.expenses.data.local.CategoryBreakdown
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseDao
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseSummary
import com.techsultan.zenithpro.features.expenses.domain.repository.ExpenseRepository
import com.techsultan.zenithpro.features.sales.data.local.SaleDao
import com.techsultan.zenithpro.features.sales.domain.repository.SaleRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class ReportsViewModel(
    private val saleDao: SaleDao,
    private val expenseDao: ExpenseDao,
    private val customerDao: CustomerDao,
    private val saleRepository: SaleRepository,
    private val expenseRepository: ExpenseRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    fun init(businessId: String) {
        loadReport(businessId)
    }

    fun onPeriodChanged(period: ReportPeriod, businessId: String) {
        _state.update { it.copy(selectedPeriod = period) }
        loadReport(businessId)
    }

    fun refresh(businessId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            if (networkMonitor.isConnected()) {
                saleRepository.pullSalesFromServer(businessId)
                expenseRepository.pullFromServer(businessId)
            }
            loadReport(businessId)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadReport(businessId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val (from, to) = _state.value.selectedPeriod.dateRange()

            val salesSummaryJob  = async { loadSalesSummary(businessId, from, to) }
            val expenseSummaryJob = async { loadExpenseSummary(businessId, from, to) }
            val chartJob = async { loadChartData(businessId, from, to) }
            val customerStatsJob = async { customerDao.getCustomerStats(businessId) }
            val topSpendersJob = async { customerDao.getTopSpenders(businessId, 5) }
            val categoryBreakJob = async { expenseDao.getCategoryBreakdown(businessId, from, to) }

            _state.update { s ->
                s.copy(
                    isLoading         = false,
                    salesSummary      = salesSummaryJob.await(),
                    expenseSummary    = expenseSummaryJob.await(),
                    chartData         = chartJob.await(),
                    customerStats     = customerStatsJob.await(),
                    topSpenders       = topSpendersJob.await(),
                    expenseBreakdown  = categoryBreakJob.await()
                )
            }
        }
    }

    private suspend fun loadSalesSummary(
        businessId: String, from: String, to: String
    ): ReportSalesSummary {
        val startOfFrom = LocalDate.parse(from)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
        val endOfTo     = LocalDate.parse(to).plusDays(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
        val summary = saleDao.getTodaySummary(businessId, startOfFrom)
        return ReportSalesSummary(
            totalRevenue  = summary.totalRevenue,
            totalProfit   = summary.totalProfit,
            totalCost     = summary.totalCost,
            totalOrders   = summary.totalOrders,
            totalDebt     = summary.totalDebt,
            profitMargin  = if (summary.totalRevenue > 0)
                (summary.totalProfit.toFloat() / summary.totalRevenue * 100) else 0f
        )
    }

    private suspend fun loadExpenseSummary(
        businessId: String, from: String, to: String
    ): ExpenseSummary =
        expenseDao.getSummary(businessId, from, to, null)

    private suspend fun loadChartData(
        businessId: String, from: String, to: String
    ): List<ChartDataPoint> {
        val since = LocalDate.parse(from)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
        return saleDao.getChartData(businessId, since)
    }
}

enum class ReportPeriod {
    TODAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, THIS_YEAR;

    fun dateRange(): Pair<String, String> {
        val today = LocalDate.now()
        return when (this) {
            TODAY      -> today.toString() to today.toString()
            THIS_WEEK  -> today.with(DayOfWeek.MONDAY).toString() to today.toString()
            THIS_MONTH -> today.withDayOfMonth(1).toString() to today.toString()
            LAST_MONTH -> today.minusMonths(1).withDayOfMonth(1).toString() to
                    today.minusMonths(1).let { it.withDayOfMonth(it.lengthOfMonth()) }.toString()
            THIS_YEAR  -> today.withDayOfYear(1).toString() to today.toString()
        }
    }

    fun label(): String = when (this) {
        TODAY      -> "Today"
        THIS_WEEK  -> "This week"
        THIS_MONTH -> "This month"
        LAST_MONTH -> "Last month"
        THIS_YEAR  -> "This year"
    }
}

data class ReportSalesSummary(
    val totalRevenue: Long  = 0L,
    val totalProfit: Long   = 0L,
    val totalCost: Long     = 0L,
    val totalOrders: Int    = 0,
    val totalDebt: Long     = 0L,
    val profitMargin: Float = 0f
)

data class ReportsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    val salesSummary: ReportSalesSummary = ReportSalesSummary(),
    val expenseSummary: ExpenseSummary = ExpenseSummary(),
    val chartData: List<ChartDataPoint> = emptyList(),
    val customerStats: CustomerStats = CustomerStats(),
    val topSpenders: List<CustomerEntity> = emptyList(),
    val expenseBreakdown: List<CategoryBreakdown> = emptyList()
)