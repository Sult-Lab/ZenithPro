package com.techsultan.zenithpro.features.product.data.local

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
    val imageUrl: String?,
    val isActive: Boolean,
    val imageUrls: List<String>,
    val expiryWarningDays: Int?,
    val updatedAt: String,
    val deletedAt: String?,
    val syncStatus: Util.SyncStatus = Util.SyncStatus.SYNCED,
    val locallyCreatedAt: String? = null
)
