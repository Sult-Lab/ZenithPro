package com.techsultan.zenithpro.features.branch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.techsultan.zenithpro.core.util.Util

@Entity(tableName = "branches")
data class BranchEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val address: String?,
    val phone: String?,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val syncStatus: Util.SyncStatus = Util.SyncStatus.SYNCED
)