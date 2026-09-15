package com.techsultan.zenithpro.features.sales.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_items",
    foreignKeys = [ForeignKey(
        entity = SaleEntity::class,
        parentColumns = ["id"],
        childColumns = ["saleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("saleId")]
)
data class SaleItemEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val variantId: String,
    val productId: String,
    val productName: String,
    val variantSku: String,
    val unitType: String = "UNIT",
    val unitPrice: Long,
    val costPrice: Long,
    val quantity: Double,
    val discount: Long,
    val totalPrice: Long
)