package com.techsultan.zenithpro.features.inventory.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProductRequest(
    @SerialName("clientId")  val clientId: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String?,
    @SerialName("category")  val category: String?,
    @SerialName("baseSalesPrice") val baseSalesPrice: Long,
    @SerialName("baseCostPrice") val baseCostPrice: Long,
    @SerialName("expiryWarningDays") val expiryWarningDays: Int? = null,
    @SerialName("isActive") val isActive: Boolean = true,
    @SerialName("variants") val variants: List<ProductVariantCreateRequest> = emptyList(),
    @SerialName("businessId") val businessId: String,
    @SerialName("imageUrls") val imageUrls: List<String> = emptyList(),
    @SerialName("defaultStock") val defaultStock: List<StockCreateRequest> = emptyList()
)
