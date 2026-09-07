package com.techsultan.zenithpro.features.inventory.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddProductRequest(
    @SerialName("clientId") val clientId: String,
    @SerialName("branchId")val branchId: String? = null,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String?,
    @SerialName("category") val category: String?,
    @SerialName("baseSalesPrice") val baseSalesPrice: Long,
    @SerialName("baseCostPrice") val baseCostPrice: Long,
    @SerialName("expiryWarningDays") val expiryWarningDays: Int? = null,
    @SerialName("unitType") val unitType: String = "UNIT",
    @SerialName("isActive") val isActive: Boolean = true,
    @SerialName("variants") val variants: List<ProductVariantCreateRequest>? = emptyList(),
    @SerialName("businessId") val businessId: String,
    @SerialName("imageUrls") val imageUrls: List<String> = emptyList(),
    @SerialName("defaultStock") val defaultStock: List<StockCreateRequest> = emptyList(),
    val updatedBy: String? = null,      // ADD — staffId from session
    val updatedByName: String? = null,
)

@Serializable
data class ProductVariantCreateRequest(
    @SerialName("clientId") val clientId: String,
    @SerialName("sku") val sku: String,
    @SerialName("salesPrice") val salesPrice: Long,
    @SerialName("costPrice") val costPrice: Long,
    @SerialName("barcode") val barcode: String?,
    @SerialName("attributes") val attributes: List<VariantAttributeInput>,
    @SerialName("stock") val stock: List<StockCreateRequest>
)

@Serializable
data class VariantAttributeInput(
    @SerialName("optionName") val optionName: String,
    @SerialName("optionValue") val optionValue: String
)

@Serializable
data class StockCreateRequest(
    @SerialName("quantity") val quantity: Double,
    @SerialName("expiryDate") val expiryDate: String?,
    @SerialName("lowStockAlert") val lowStockAlert: Int? = null
)


@Serializable
data class ImageUploadRequest(
    @SerialName("imageBase64") val imageBase64: String,
    @SerialName("businessId") val businessId: String
)
