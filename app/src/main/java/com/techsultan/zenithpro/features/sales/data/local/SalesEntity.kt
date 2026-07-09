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
    val status: SaleStatus,
    val notes: String?,
    val soldAt: String,
    val updatedAt: String,
    val syncStatus: Util.SyncStatus = Util.SyncStatus.PENDING
)
