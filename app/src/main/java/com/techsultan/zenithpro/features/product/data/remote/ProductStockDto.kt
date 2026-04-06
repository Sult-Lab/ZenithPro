package com.techsultan.zenithpro.features.product.data.remote

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.product.data.local.ProductVariantEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductStockDto(
    val id: String,
    @SerialName("variant_id")      val variantId: String,
    val quantity: Int,
    @SerialName("expiry_date")     val expiryDate: String?,
    @SerialName("low_stock_alert") val lowStockAlert: Int?,
    @SerialName("updated_at")      val updatedAt: String
)

