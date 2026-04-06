package com.techsultan.zenithpro.features.settings.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusinessSettingsDto(
    @SerialName("business_id") val businessId: String,
    @SerialName("currency_symbol") val currencySymbol: String,
    @SerialName("currency_code") val currencyCode: String,
    @SerialName("tax_rate") val taxRate: Double,
    @SerialName("allow_negative_stock") val allowNegativeStock: Boolean,
    @SerialName("require_customer_sale") val requireCustomerSale: Boolean,
    @SerialName("low_stock_threshold") val lowStockThreshold: Int,
    @SerialName("receipt_footer") val receiptFooter: String? = null,
    @SerialName("updated_at") val updatedAt: String
)