package com.techsultan.zenithpro.features.material.data.remote

import com.techsultan.zenithpro.features.material.data.local.RecipeEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecipeDto(
    val id: String,
    @SerialName("business_id") val businessId: String,
    @SerialName("variant_id") val variantId: String,
    @SerialName("material_id") val materialId: String,
    @SerialName("quantity_needed") val quantityNeeded: Double,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
) {
    fun toEntity() = RecipeEntity(
        id = id,
        businessId = businessId,
        variantId = variantId,
        materialId = materialId,
        quantityNeeded = quantityNeeded,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}