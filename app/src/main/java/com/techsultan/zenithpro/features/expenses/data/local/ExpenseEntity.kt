package com.techsultan.zenithpro.features.expenses.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.techsultan.zenithpro.core.util.Util

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val branchId: String?,
    val recordedBy: String,
    val title: String,
    val amount: Long,
    val category: String,
    val notes: String?,
    val receiptUrl: String?,
    val expenseDate: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val syncStatus: Util.SyncStatus = Util.SyncStatus.SYNCED
)