package com.techsultan.zenithpro.features.inventory.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.techsultan.zenithpro.core.util.Util

@Entity(
    tableName = "product_stock",
    foreignKeys = [
        ForeignKey(
            entity = ProductVariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["variantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("variantId"),
        Index("branchId"),      // ADD
    ]
)
data class ProductStockEntity(
    @PrimaryKey val id: String,
    val variantId: String,
    val branchId: String,       // ADD
    val quantity: Int,
    val expiryDate: String?,
    val lowStockAlert: Int?,
    val updatedAt: String,
    val deletedAt: String? = null,
    val syncStatus: Util.SyncStatus = Util.SyncStatus.SYNCED,
)