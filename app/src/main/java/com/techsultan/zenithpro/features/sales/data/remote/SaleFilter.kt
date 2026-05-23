package com.techsultan.zenithpro.features.sales.data.remote

data class SaleFilter(
    val from: String,
    val to: String,
    val staffId: String? = null,
    val paymentMethod: String? = null,
    val branchId: String? = null
)
