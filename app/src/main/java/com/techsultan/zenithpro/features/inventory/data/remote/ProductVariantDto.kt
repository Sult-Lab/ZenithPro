package com.techsultan.zenithpro.features.inventory.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductVariantDto(
    val id: String,
    @SerialName("product_id")   val productId: String,
    @SerialName("business_id")  val businessId: String,
    val sku: String,
    @SerialName("sales_price")  val salesPrice: Long,
    @SerialName("cost_price")   val costPrice: Long,
    val barcode: String? = null,
    @SerialName("updated_at")   val updatedAt: String,
    @SerialName("deleted_at")   val deletedAt: String? = null
)

