package com.techsultan.zenithpro.features.material.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.techsultan.zenithpro.core.util.Util

@Entity(tableName = "materials")
data class MaterialEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val description: String?,
    val unit: String,
    val quantity: Double,
    val costPerUnit: Long,
    val lowStockAlert: Double?,
    val status: String,
    val supplier: String?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val syncStatus: Util.SyncStatus = Util.SyncStatus.SYNCED
) {
    val isLowStock: Boolean
        get() = lowStockAlert != null && quantity <= lowStockAlert
}