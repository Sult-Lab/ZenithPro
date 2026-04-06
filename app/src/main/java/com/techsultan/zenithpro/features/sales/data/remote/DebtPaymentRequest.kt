package com.techsultan.zenithpro.features.sales.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class DebtPaymentRequest(
    val saleId: String,
    val customerId: String,
    val amount: Long,
    val paymentMethod: String = "CASH",
    val notes: String? = null
)