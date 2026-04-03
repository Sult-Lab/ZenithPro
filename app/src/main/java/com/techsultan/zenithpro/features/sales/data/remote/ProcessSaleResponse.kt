package com.techsultan.zenithpro.features.sales.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ProcessSaleResponse(
    val saleId: String,
    val status: String,
    val debtAmount: Long,
    val idempotent: Boolean
)