package com.techsultan.zenithpro.features.sales.domain.model

data class NombaSummary(
    val totalReceivedToday: Long,
    val percentageChange: Double,
    val isConnected: Boolean,
    val terminalName: String,
    val terminalId: String
)
