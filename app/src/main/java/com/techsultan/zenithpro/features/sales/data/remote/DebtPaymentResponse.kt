package com.techsultan.zenithpro.features.sales.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class DebtPaymentResponse(
    val paymentId: String,
    val newStatus: String,
    val remainingDebt: Long
)