package com.techsultan.zenithpro.features.inventory.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: String,
    @SerialName("business_id")
    val businessId: String,
    val name: String,
    val description: String?,
    val category: String?,
    @SerialName("base_sales_price")
    val baseSalesPrice: Long,
    @SerialName("base_cost_price")
    val baseCostPrice: Long,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("is_active")
    val isActive: Boolean,
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("expiry_warning_days") val expiryWarningDays: Int? = null
)



