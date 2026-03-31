package com.techsultan.zenithpro.features.sales.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class SaleItemRequest(
    val variantId: String,
    val productId: String,
    val productName: String,
    val variantSku: String,
    val unitPrice: Long,
    val costPrice: Long,
    val quantity: Int,
    val discount: Long
)