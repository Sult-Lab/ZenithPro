package com.techsultan.zenithpro.features.dashboard.data.remote

data class DashboardSummary(
    val totalRevenue: Long   = 0L,
    val totalCollected: Long = 0L,
    val totalDebt: Long      = 0L,
    val totalOrders: Int     = 0,
    val totalProfit: Long    = 0L,
    val totalCost: Long      = 0L
) {
    val profitMargin: Float
        get() = if (totalRevenue == 0L) 0f
        else (totalProfit.toFloat() / totalRevenue.toFloat()) * 100f
}