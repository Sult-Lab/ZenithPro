package com.techsultan.zenithpro.features.sales.data.remote

import com.techsultan.zenithpro.core.data.local.SplitPayment
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity

data class CompletedSale(
    val saleId: String,
    val receiptNumber: String,
    val salesPerson: String,
    val paymentMethod: String,
    val subtotal: Long,
    val discount: Long,
    val total: Long,
    val amountPaid: Long,
    val change: Long,
    val cartItems: List<CartItem>,
    val customer: CustomerEntity?,
    val splitPayments: List<SplitPayment>,
    val createdAt: Long,
    val businessName: String,
    val businessAddress: String,
    val businessNumber: String,
    val taxRate: Double = 0.0,
    val footerMessage: String? = null,
    val businessLogo: String? = null
)