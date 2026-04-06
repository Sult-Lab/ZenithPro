package com.techsultan.zenithpro.features.dashboard.data.remote

data class ChartDataPoint(
    val saleDate: String,   // "2025-03-24"
    val revenue: Long,
    val profit: Long,
    val orderCount: Int
)