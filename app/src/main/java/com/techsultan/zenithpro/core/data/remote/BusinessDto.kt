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
    @SerialName("currency_symbol") val currencySymbol: String = "₦"
)