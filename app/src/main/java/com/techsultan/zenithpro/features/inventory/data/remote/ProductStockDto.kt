package com.techsultan.zenithpro.features.inventory.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductStockDto(
    val id: String,
    @SerialName("variant_id")      val variantId: String,
    val quantity: Int,
    @SerialName("expiry_date")     val expiryDate: String? = null,
    @SerialName("low_stock_alert") val lowStockAlert: Int? = null,
    @SerialName("updated_at")      val updatedAt: String,
    @SerialName("branch_id") val branchId: String
)

