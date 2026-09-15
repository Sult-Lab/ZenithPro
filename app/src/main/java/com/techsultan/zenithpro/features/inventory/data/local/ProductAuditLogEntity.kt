package com.techsultan.zenithpro.features.inventory.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_audit_log")
data class ProductAuditLogEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val businessId: String,
    val changedBy: String,
    val changedByName: String,
    val changedByRole: String,
    val changeType: String,
    val fieldChanged: String? = null,
    val oldValue: String? = null,
    val newValue: String? = null,
    val variantSku: String? = null,
    val notes: String? = null,
    val createdAt: String,
)