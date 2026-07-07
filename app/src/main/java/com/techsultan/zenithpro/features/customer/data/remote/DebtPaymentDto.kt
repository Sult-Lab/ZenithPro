package com.techsultan.zenithpro.features.customer.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DebtPaymentDto(
    val id: String,
    @SerialName("business_id") val businessId: String,
    @SerialName("sale_id") val saleId: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("staff_id") val staffId: String,
    val amount: Long,
    @SerialName("payment_method") val paymentMethod: String,
    val notes: String?,
    @SerialName("paid_at") val paidAt: String
)