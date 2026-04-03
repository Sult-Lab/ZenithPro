package com.techsultan.zenithpro.features.sales.data.remote

data class DailySummary(
    val totalRevenue: Long,
    val totalCollected: Long,
    val totalDebt: Long,
    val orderCount: Int
)