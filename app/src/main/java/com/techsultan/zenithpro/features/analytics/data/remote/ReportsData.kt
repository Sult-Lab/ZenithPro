package com.techsultan.zenithpro.features.analytics.data.remote

import com.techsultan.zenithpro.features.analytics.data.ReportPeriod
import com.techsultan.zenithpro.features.analytics.data.ReportSalesSummary
import com.techsultan.zenithpro.features.analytics.data.TopProductRow
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.data.remote.CustomerStats
import com.techsultan.zenithpro.features.dashboard.data.remote.ChartDataPoint
import com.techsultan.zenithpro.features.expenses.data.local.CategoryBreakdown
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseSummary

data class ReportsData(
    val period: ReportPeriod,
    val salesSummary: ReportSalesSummary,
    val expenseSummary: ExpenseSummary,
    val chartData: List<ChartDataPoint>,
    val expenseBreakdown: List<CategoryBreakdown>,
    val topProducts: List<TopProductRow>,
    val staffPerformance: List<StaffPerformance>,
    val customerStats: CustomerStats,
    val topSpenders: List<CustomerEntity>,
    val availableBranches: List<BranchEntity>
)