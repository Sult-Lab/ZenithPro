package com.techsultan.zenithpro.features.analytics.data

data class ReportSalesSummary(
    val totalRevenue: Long  = 0L,
    val totalCollected: Long = 0L,
    val totalProfit: Long   = 0L,
    val totalCost: Long     = 0L,
    val totalOrders: Int    = 0,
    val totalDebt: Long     = 0L,
    val profitMargin: Float = 0f
)