package com.techsultan.zenithpro.features.auth.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class SignUpRequest(
    val email: String,
    val password: String,
    val confirmPassword: String,

    val adminFirstName: String,
    val adminLastName: String,

    val businessName: String,
    val businessPhone: String?,
    val businessAddress: String?,
    val businessLogoUrl: String? = "",

    val acceptTerms: Boolean
)

