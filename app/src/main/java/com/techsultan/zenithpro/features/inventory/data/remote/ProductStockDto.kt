package com.techsultan.zenithpro.features.inventory.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductStockDto(
    val id: String,
    @SerialName("variant_id")      val variantId: String,
    val quantity: Int,
    @SerialName("expiry_date")     val expiryDate: String?,
    @SerialName("low_stock_alert") val lowStockAlert: Int?,
    @SerialName("updated_at")      val updatedAt: String,
    @SerialName("branchId") val branchId: String
)

