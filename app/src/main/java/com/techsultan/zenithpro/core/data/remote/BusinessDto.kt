package com.techsultan.zenithpro.core.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusinessDto(
    val id: String,
    val name: String,
    val type: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    @SerialName("logo_url") val logoUrl: String?,
    @SerialName("currency_code")  val currencyCode: String  = "NGN",
    @SerialName("currency_symbol") val currencySymbol: String = "₦",
    @SerialName("account_number") val accountNumber: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("account_name") val accountName: String? = null
)
