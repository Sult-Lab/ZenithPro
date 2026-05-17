package com.techsultan.zenithpro.features.category.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.techsultan.zenithpro.core.util.Util

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val color: String?,
    val icon: String?,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val syncStatus: Util.SyncStatus = Util.SyncStatus.SYNCED
)