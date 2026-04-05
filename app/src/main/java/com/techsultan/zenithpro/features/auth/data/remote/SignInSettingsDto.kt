package com.techsultan.zenithpro.features.auth.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class SignInSettingsDto(
    val currencySymbol: String,
    val currencyCode: String,
    val taxRate: Double,
    val allowNegativeStock: Boolean,
    val requireCustomerSale: Boolean,
    val lowStockThreshold: Int
)