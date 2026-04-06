package com.techsultan.zenithpro.features.product.data.remote

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.product.data.local.ProductVariantEntity
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
    val barcode: String?,
    @SerialName("updated_at")   val updatedAt: String,
    @SerialName("deleted_at")   val deletedAt: String? = null
)

