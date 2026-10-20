package com.techsultan.zenithpro.features.material.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdjustMaterialStockRequest(
    @SerialName("p_material_id")
    val materialId: String,

    @SerialName("p_quantity")
    val quantity: Double,

    @SerialName("p_staff_id")
    val staffId: String,

    @SerialName("p_business_id")
    val businessId: String,

    @SerialName("p_notes")
    val notes: String? = null
)