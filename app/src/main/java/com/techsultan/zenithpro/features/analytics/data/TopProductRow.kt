package com.techsultan.zenithpro.features.analytics.data

data class TopProductRow(
    val productName: String,
    val unitsSold: Int,
    val revenue: Long,
    val profit: Long
)