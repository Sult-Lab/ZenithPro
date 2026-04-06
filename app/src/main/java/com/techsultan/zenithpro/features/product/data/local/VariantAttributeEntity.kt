package com.techsultan.zenithpro.features.product.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.techsultan.zenithpro.core.util.Util

@Entity(
    tableName = "variant_attributes",
    foreignKeys = [ForeignKey(
        entity = ProductVariantEntity::class,
        parentColumns = ["id"],
        childColumns = ["variantId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("variantId")]
)
data class VariantAttributeEntity(
    @PrimaryKey val id: String,
    val variantId: String,
    val optionName: String,
    val optionValue: String
)