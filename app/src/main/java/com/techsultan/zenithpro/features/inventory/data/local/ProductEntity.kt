package com.techsultan.zenithpro.features.inventory.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.techsultan.zenithpro.core.util.Util

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val description: String?,
    val category: String?,
    val baseSalesPrice: Long,
    val baseCostPrice: Long,
    val isActive: Boolean,
    val imageUrls: List<String>,
    val expiryWarningDays: Int?,
    val unitType: String = "UNIT",
    val updatedAt: String,
    val deletedAt: String?,
    val syncStatus: Util.SyncStatus = Util.SyncStatus.SYNCED,
    val locallyCreatedAt: String? = null,
    val branchId: String?,
    val updatedBy: String? = null,
    val updatedByName: String? = null,
)
