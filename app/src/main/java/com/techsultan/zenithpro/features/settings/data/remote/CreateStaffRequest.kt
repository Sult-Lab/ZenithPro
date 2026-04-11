package com.techsultan.zenithpro.features.settings.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CreateStaffRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String?,
    val phone: String?,
    val role: String,
    val branchId: String?
)

