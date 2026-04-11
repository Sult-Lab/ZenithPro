package com.techsultan.zenithpro.features.settings.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CreateStaffResponse(
    val staffId: String,
    val email: String?,
    val firstName: String,
    val lastName: String?,
    val role: String,
    val branchId: String?,
    val message: String
)
