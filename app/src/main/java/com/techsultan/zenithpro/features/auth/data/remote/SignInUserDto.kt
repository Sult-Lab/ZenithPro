package com.techsultan.zenithpro.features.auth.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class SignInUserDto(
    val id: String,
    val email: String?,
    val firstName: String,
    val lastName: String?,
    val role: String,
    val mustChangePassword: Boolean
)