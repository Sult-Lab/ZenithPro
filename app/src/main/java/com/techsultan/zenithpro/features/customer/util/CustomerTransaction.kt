package com.techsultan.zenithpro.features.customer.util

import com.techsultan.zenithpro.features.customer.data.local.DebtPaymentEntity
import com.techsultan.zenithpro.features.sales.data.local.SaleEntity
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems

sealed class CustomerTransaction {
    data class Sale(
        val saleWithItems: SaleWithItems
    ) : CustomerTransaction() {
        val date: String get() = saleWithItems.sale.soldAt
    }

    data class DebtPayment(
        val payment: DebtPaymentEntity,
        val originalSale: SaleEntity?
    ) : CustomerTransaction() {
        val date: String get() = payment.paidAt
    }

    // Sort key for chronological ordering
    val sortDate: String get() = when (this) {
        is Sale        -> date
        is DebtPayment -> date
    }
}