package com.techsultan.zenithpro.features.inventory.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.techsultan.zenithpro.core.util.Util

@Entity(
    tableName = "product_stock",
    foreignKeys = [ForeignKey(
        entity = ProductVariantEntity::class,
        parentColumns = ["id"],
        childColumns = ["variantId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("variantId")]
)
data class ProductStockEntity(
    @PrimaryKey val id: String,
    val variantId: String,
    val quantity: Int,
    val expiryDate: String?,
    val lowStockAlert: Int?,
    val updatedAt: String,
    val syncStatus: Util.SyncStatus = Util.SyncStatus.SYNCED
)