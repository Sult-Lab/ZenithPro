package com.techsultan.zenithpro.core.data.local

import com.techsultan.zenithpro.core.util.Util

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
    val createdAt: Long,
    val printedAt: Long = System.currentTimeMillis(),
    val businessName: String,
    val businessAddress: String,
    val businessNumber: String,
    val taxRate: Double = 0.0,
    val footerMessage: String? = null,
    val businessLogo: String?,
)

data class ReceiptItem(
    val name: String,
    val qty: Double,
    val unitType: String = "UNIT",
    val price: Long,
    val total: Long
)

data class SplitPayment(
    val method: String,
    val amount: Long
)

data class PrinterDevice(
    val id: String,
    val name: String,
    val type: Util.PrinterType,
    val address: String? = null
)