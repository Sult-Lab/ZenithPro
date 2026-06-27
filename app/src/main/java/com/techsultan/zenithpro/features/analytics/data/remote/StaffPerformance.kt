package com.techsultan.zenithpro.features.analytics.data.remote

data class StaffPerformance(
    val staffId: String,
    val staffName: String,
    val orderCount: Int,
    val revenue: Long
)