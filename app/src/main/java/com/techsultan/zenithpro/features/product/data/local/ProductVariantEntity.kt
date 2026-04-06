package com.techsultan.zenithpro.features.product.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.techsultan.zenithpro.core.util.Util

@Entity(
    tableName = "product_variants",
    foreignKeys = [ForeignKey(
        entity = ProductEntity::class,
        parentColumns = ["id"],
        childColumns = ["productId"],
        onDelete = ForeignKey.CASCADE   // clean up variants when product is hard-deleted
    )],
    indices = [Index("productId")]
)
data class ProductVariantEntity(
    @PrimaryKey val id: String,
    val productId: String,              // FK → products.id
    val businessId: String,
    val sku: String,
    val salesPrice: Long,
    val costPrice: Long,
    val barcode: String?,
    val updatedAt: String,
    val deletedAt: String?,
    val syncStatus: Util.SyncStatus = Util.SyncStatus.SYNCED
)