package com.techsultan.zenithpro.features.sales.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SaleItemDto(
    val id: String,
    @SerialName("sale_id")      val saleId: String,
    @SerialName("variant_id")   val variantId: String,
    @SerialName("product_id")   val productId: String,
    @SerialName("product_name") val productName: String,
    @SerialName("variant_sku")  val variantSku: String,
    @SerialName("unit_price")   val unitPrice: Long,
    @SerialName("cost_price")   val costPrice: Long,
    val quantity: Double,
    val discount: Long,
    @SerialName("total_price")  val totalPrice: Long
)