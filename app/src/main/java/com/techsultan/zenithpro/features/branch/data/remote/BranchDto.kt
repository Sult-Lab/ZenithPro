package com.techsultan.zenithpro.features.branch.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BranchDto(
    val id: String,
    @SerialName("business_id") val businessId: String,
    val name: String,
    val address: String?,
    val phone: String?,
    @SerialName("is_active")   val isActive: Boolean,
    @SerialName("created_at")  val createdAt: String,
    @SerialName("updated_at")  val updatedAt: String,
    @SerialName("deleted_at")  val deletedAt: String? = null
)