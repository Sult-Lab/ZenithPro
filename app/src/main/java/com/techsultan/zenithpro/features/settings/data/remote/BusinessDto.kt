package com.techsultan.zenithpro.features.settings.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class UpdateBusinessRequest(
    val name: String,
    val type: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val logoUrl: String?,
    val currencyCode: String,
    val currencySymbol: String
)

// Response DTO
@Serializable
data class UpdateBusinessResponse(
    val businessId: String,
    val name: String,
    val type: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val logoUrl: String?,
    val currencyCode: String,
    val currencySymbol: String,
    val updatedAt: String
)