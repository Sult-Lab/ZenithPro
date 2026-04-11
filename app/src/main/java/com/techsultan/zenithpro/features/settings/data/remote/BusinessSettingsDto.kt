package com.techsultan.zenithpro.features.settings.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class BusinessSettingsDto(
    @SerialName("business_id") val businessId: String,
    @SerialName("currency_symbol") val currencySymbol: String= "₦",
    @SerialName("currency_code") val currencyCode: String = "NGN",
    @SerialName("tax_rate") val taxRate: Double = 0.0,
    @SerialName("allow_negative_stock") val allowNegativeStock: Boolean  = false,
    @SerialName("require_customer_sale") val requireCustomerSale: Boolean = false,
    @SerialName("low_stock_threshold") val lowStockThreshold: Int = 5,
    @SerialName("receipt_footer") val receiptFooter: String? = null,
    @SerialName("updated_at") val updatedAt: String = Instant.now().toString()
)