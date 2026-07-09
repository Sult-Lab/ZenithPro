package com.techsultan.zenithpro.features.sales.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SaleDto(
    val id: String,
    @SerialName("client_transaction_id") val clientTransactionId: String,
    @SerialName("business_id")  val businessId: String,
    @SerialName("branch_id")    val branchId: String?,
    @SerialName("customer_id")  val customerId: String?,
    @SerialName("staff_id")     val staffId: String,
    val subtotal: Long,
    @SerialName("discount_amount") val discountAmount: Long,
    @SerialName("tax_amount")   val taxAmount: Long,
    @SerialName("total_amount") val totalAmount: Long,
    @SerialName("amount_paid")  val amountPaid: Long,
    @SerialName("change_amount") val changeAmount: Long,
    @SerialName("debt_amount")  val debtAmount: Long,
    @SerialName("payment_method") val paymentMethod: String,
    val status: String,
    val notes: String?,
    @SerialName("sold_at")      val soldAt: String,
    @SerialName("updated_at")   val updatedAt: String,
    @SerialName("terminal_id")  val terminalId: String? = null,
)