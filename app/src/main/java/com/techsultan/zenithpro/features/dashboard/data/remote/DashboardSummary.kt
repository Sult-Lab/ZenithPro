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

data class DashboardFulfillmentSummary(
    val totalOrdersThisWeek: Int = 0,
    val pendingCount: Int = 0,
    // TODO: "processed" bucket maps to PROCESSING — unconfirmed with business owner if "processed" means PROCESSING or DELIVERED.
    val processingCount: Int = 0,
    val deliveredCount: Int = 0,
    val failedCount: Int = 0,
    val cancelledCount: Int = 0
)
