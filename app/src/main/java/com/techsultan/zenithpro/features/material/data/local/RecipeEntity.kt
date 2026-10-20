package com.techsultan.zenithpro.features.material.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.techsultan.zenithpro.core.util.Util

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
    val syncStatus: Util.SyncStatus = Util.SyncStatus.SYNCED,
    val createdAt: String,
    val updatedAt: String,
)