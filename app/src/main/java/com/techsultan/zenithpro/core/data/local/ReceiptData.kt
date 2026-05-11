package com.techsultan.zenithpro.core.data.local

data class ReceiptData(
    val receiptNumber: String,
    val cashierName: String,
    val items: List<ReceiptItem>,
    val subtotal: Long,
    val discount: Long,
    val total: Long,
    val amountPaid: Long,
    val change: Long,
    val paymentMethod: String,
    val customerName: String?,
    val splitPayments: List<SplitPayment> = emptyList(),
    val createdAt: Long
)

data class ReceiptItem(
    val name: String,
    val qty: Int,
    val price: Long,
    val total: Long
)

data class SplitPayment(
    val method: String,
    val amount: Long
)