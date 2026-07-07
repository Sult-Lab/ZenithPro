package com.techsultan.zenithpro.features.payment.data.remote

import com.techsultan.zenithpro.features.sales.data.local.PaymentEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentDto(
    val id: String,
    @SerialName("sale_id") val saleId: String,
    @SerialName("business_id") val businessId: String,
    val provider: String,
    val status: String,
    val amount: Long,
    val currency: String,
    @SerialName("provider_reference") val providerReference: String? = null,
    @SerialName("checkout_reference") val checkoutReference: String? = null,
    @SerialName("authorization_code") val authorizationCode: String? = null,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("transfer_type") val transferType: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

fun PaymentDto.toEntity() = PaymentEntity(
    id = id,
    saleId = saleId,
    businessId = businessId,
    provider = provider,
    status = status,
    amount = amount,
    currency = currency,
    providerReference = providerReference,
    checkoutReference = checkoutReference,
    authorizationCode = authorizationCode,
    paymentMethod = paymentMethod,
    transferType = transferType,
    syncStatus = "SYNCED",
    createdAt = createdAt,
    updatedAt = updatedAt
)
