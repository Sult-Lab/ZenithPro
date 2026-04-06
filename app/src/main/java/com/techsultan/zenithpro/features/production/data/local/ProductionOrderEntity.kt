package com.techsultan.zenithpro.features.production.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.techsultan.zenithpro.core.util.Util

@Entity(tableName = "production_orders")
data class ProductionOrderEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val branchId: String?,
    val variantId: String,
    val quantity: Double,
    val status: String,
    val notes: String?,
    val startedAt: String?,
    val completedAt: String?,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String,
    val syncStatus: Util.SyncStatus = Util.SyncStatus.SYNCED
)