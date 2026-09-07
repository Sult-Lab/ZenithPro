package com.techsultan.zenithpro.features.inventory.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductAuditLogDto(
    val id: String,
    @SerialName("product_id")
    val productId: String,
    @SerialName("business_id")
    val businessId: String,
    @SerialName("changed_by")
    val changedBy: String,
    @SerialName("changed_by_name")
    val changedByName: String,
    @SerialName("changed_by_role")
    val changedByRole: String,
    @SerialName("change_type")
    val changeType: String,
    @SerialName("field_changed")
    val fieldChanged: String? = null,
    @SerialName("old_value")
    val oldValue: String? = null,
    @SerialName("new_value")
    val newValue: String? = null,
    @SerialName("variant_sku")
    val variantSku: String? = null,
    val notes: String? = null,
    @SerialName("created_at")
    val createdAt: String,
)
