package com.techsultan.zenithpro.features.auth.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CreateStaffRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val temporaryPassword: String
)

@Serializable
data class CreateStaffResponse(
    val staffUserId: String
)
