package com.techsultan.zenithpro.features.settings.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class EditStaffRequest(
    val staffId: String,
    val firstName: String,
    val lastName: String?,
    val phone: String?,
    val email: String?,
    val role: String,
    val branchId: String?,
    val status: String
)