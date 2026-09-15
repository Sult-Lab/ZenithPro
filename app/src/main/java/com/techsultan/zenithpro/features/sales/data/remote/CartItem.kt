package com.techsultan.zenithpro.features.sales.data.remote

data class CartItem(
    val variantId: String,
    val productId: String,
    val productName: String,
    val variantSku: String,
    val unitType: String = "UNIT",
    val unitPrice: Long,
    val costPrice: Long,
    var quantity: Double,
    var discount: Long = 0L
) {
    val totalPrice: Long
        get() = ((unitPrice - discount) * quantity).toLong()
}