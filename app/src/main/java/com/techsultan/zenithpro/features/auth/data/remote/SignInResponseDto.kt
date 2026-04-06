package com.techsultan.zenithpro.features.auth.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class SignInResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val user: SignInUserDto,
    val business: SignInBusinessDto,
    val settings: SignInSettingsDto
)