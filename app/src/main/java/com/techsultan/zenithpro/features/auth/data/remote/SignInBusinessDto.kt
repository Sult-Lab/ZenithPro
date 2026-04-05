package com.techsultan.zenithpro.features.auth.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class SignInBusinessDto(
    val id: String,
    val name: String,
    val phone: String?,
    val address: String?
)