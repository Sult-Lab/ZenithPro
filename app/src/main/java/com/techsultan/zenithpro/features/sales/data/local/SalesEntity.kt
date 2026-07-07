package com.techsultan.zenithpro.features.sales.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.SaleStatus

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String,
    val clientTransactionId: String,
    val businessId: String,
    val branchId: String?,
    val terminalId: String?,
    val customerId: String?,
    val staffId: String,
    val subtotal: Long,
    val discountAmount: Long,
    val taxAmount: Long,
    val totalAmount: Long,
    val amountPaid: Long,
    val changeAmount: Long,
    val debtAmount: Long,
    val paymentMethod: PaymentMethod,
    val transferType: String? = null,
    val status: SaleStatus,
    val paymentStatus: String,                 // ← new: COMPLETED | AWAITING_PAYMENT
    val paymentReference: String?,             // ← new: ZP-XXXXX
    val nombaPaymentReference: String?,        // ← new: Nomba's txn ID from webhook
    val paymentConfirmedAt: String?,
    val virtualAccountNumber: String? = null,
    val virtualAccountBank: String?   = null,
    val virtualAccountName: String?   = null,
    val notes: String?,
    val soldAt: String,
    val syncStatus: Util.SyncStatus = Util.SyncStatus.PENDING
){
    val isAwaitingPayment: Boolean
        get() = paymentStatus == "AWAITING_PAYMENT"
}
