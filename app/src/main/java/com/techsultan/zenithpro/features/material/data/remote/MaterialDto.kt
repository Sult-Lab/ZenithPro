package com.techsultan.zenithpro.features.material.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MaterialDto(
    val id: String,
    @SerialName("business_id") val businessId: String,
    val name: String,
    val description: String?,
    val unit: String,
    val quantity: Double,
    @SerialName("cost_per_unit") val costPerUnit: Long,
    @SerialName("low_stock_alert") val lowStockAlert: Double?,
    val status: String,
    val supplier: String?,
    val notes: String?,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)