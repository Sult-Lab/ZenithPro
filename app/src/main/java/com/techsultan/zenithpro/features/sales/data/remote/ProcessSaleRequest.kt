package com.techsultan.zenithpro.features.sales.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ProcessSaleRequest(
    val clientTransactionId: String,
    val branchId: String?,
    val customerId: String?,
    val items: List<SaleItemRequest>,
    val subtotal: Long,
    val discountAmount: Long,
    val taxAmount: Long,
    val totalAmount: Long,
    val amountPaid: Long,
    val changeAmount: Long,
    val paymentMethod: String,
    val notes: String?
)