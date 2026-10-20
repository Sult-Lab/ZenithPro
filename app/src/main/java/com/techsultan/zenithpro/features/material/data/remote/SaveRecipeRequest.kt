package com.techsultan.zenithpro.features.material.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecipeInsertRequest(
    val id: String,

    @SerialName("business_id")
    val businessId: String,

    @SerialName("variant_id")
    val variantId: String,

    @SerialName("material_id")
    val materialId: String,

    @SerialName("quantity_needed")
    val quantityNeeded: Double,

    val notes: String?,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class RecipeUpdateRequest(
    @SerialName("quantity_needed")
    val quantityNeeded: Double,
    @SerialName("updated_at")
    val updatedAt: String
)