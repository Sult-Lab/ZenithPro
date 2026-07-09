package com.techsultan.zenithpro.features.sales.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NombaTransactionDto(
    val id: String,
    @SerialName("terminal_id")  val terminalId: String,
    @SerialName("business_id") val businessId: String,
    val amount: Long,                       // kobo
    @SerialName("sender_name")  val senderName: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("virtual_account_number") val virtualAccountNumber: String? = null,
    val narration: String? = null,
    @SerialName("paid_at")  val paidAt: String,
    @SerialName("created_at") val createdAt: String,
    val confirmed: Boolean = false,
)