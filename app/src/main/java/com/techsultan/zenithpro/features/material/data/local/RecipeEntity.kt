package com.techsultan.zenithpro.features.material.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recipes",
    foreignKeys = [ForeignKey(
        entity = MaterialEntity::class,
        parentColumns = ["id"],
        childColumns = ["materialId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("variantId"), Index("materialId")]
)
data class RecipeEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val variantId: String,
    val materialId: String,
    val quantityNeeded: Double,
    val notes: String?,
    val createdAt: String
)