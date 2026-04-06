package com.techsultan.zenithpro.features.production.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductionOrderDto(
    val id: String,
    @SerialName("business_id") val businessId: String,
    @SerialName("branch_id") val branchId: String?,
    @SerialName("variant_id") val variantId: String,
    val quantity: Double,
    val status: String,
    val notes: String?,
    @SerialName("started_at") val startedAt: String?,
    @SerialName("completed_at") val completedAt: String?,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)