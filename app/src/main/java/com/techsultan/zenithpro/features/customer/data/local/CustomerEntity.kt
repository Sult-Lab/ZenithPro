package com.techsultan.zenithpro.features.customer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.techsultan.zenithpro.core.util.Util

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val firstName: String,
    val lastName: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val totalSpent: Long,
    val totalDebt: Long,
    val visitCount: Int,
    val lastVisitAt: String?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val syncStatus: Util.SyncStatus = Util.SyncStatus.SYNCED
) {
    val fullName: String get() = "$firstName ${lastName.orEmpty()}".trim()
}