package com.techsultan.zenithpro.features.product.data.remote

import com.techsultan.zenithpro.core.data.Money
import kotlinx.serialization.Serializable

@Serializable
data class AddProductRequest(
    val name: String,
    val description: String?,
    val category: String?,
    val baseSalesPrice: Long,
    val baseCostPrice: Long,
    val expiryWarningDays: Int? = null,
    val isActive: Boolean = true,
    val variants: List<ProductVariantCreateRequest>? = emptyList(),
    val businessId: String,
    val imageUrls: List<String> = emptyList(),
)


@Serializable
data class ProductVariantCreate(
    val sku: String,
    val salesPrice: Long,
    val costPrice: Long,
    val barcode: String?,
    val attributes: List<VariantAttributeInput>,
    val stock: List<StockCreateRequest>
)

@Serializable
data class VariantAttributeInput(
    val optionName: String,  // "Color"
    val optionValue: String  // "Red"
)

@Serializable
data class ProductVariantCreateRequest(
    val sku: String,
    val salesPrice: Long,
    val costPrice: Long,
    val barcode: String?,
    val attributes: List<VariantAttributeInput>,
    val stock: List<StockCreateRequest>
)

@Serializable
data class StockCreateRequest(
    val variantId: String? = null,
    val quantity: Int,
    val expiryDate: String?,
    val lowStockAlert: Int? = null
)

@Serializable
data class CreateVariantsRequest(
    val productId: String,
    val variants: List<ProductVariantCreate>
)

@Serializable
data class ImageUploadRequest(
    val imageBase64: String,
    val businessId: String
)
