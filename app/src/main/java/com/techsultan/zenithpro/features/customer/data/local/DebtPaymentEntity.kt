package com.techsultan.zenithpro.features.customer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.sales.data.local.SaleEntity

@Entity(
    tableName = "debt_payments",
    foreignKeys = [ForeignKey(
        entity = SaleEntity::class,
        parentColumns = ["id"],
        childColumns = ["saleId"],
        onDelete = ForeignKey.Companion.CASCADE
    )],
    indices = [Index("saleId"), Index("customerId")]
)
data class DebtPaymentEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val saleId: String,
    val customerId: String,
    val staffId: String,
    val amount: Long,
    val paymentMethod: String,
    val notes: String?,
    val paidAt: String,
    val syncStatus: Util.SyncStatus = Util.SyncStatus.SYNCED
)