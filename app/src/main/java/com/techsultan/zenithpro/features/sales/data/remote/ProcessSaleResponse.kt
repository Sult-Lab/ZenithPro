package com.techsultan.zenithpro.features.sales.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProcessSaleResponse(
    @SerialName("id")
    val saleId: String = "",
    @SerialName("paymentStatus")
    val status: String = "",
    @SerialName("debt_amount")
    val debtAmount: Long = 0,
    val idempotent: Boolean = false,
    val virtualAccountNumber: String? = null,
    val virtualAccountBank: String?   = null,
    val virtualAccountName: String?   = null,
)