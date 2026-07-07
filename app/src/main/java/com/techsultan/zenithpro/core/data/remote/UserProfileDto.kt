package com.techsultan.zenithpro.core.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: String,
    @SerialName("business_id") val businessId: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("email") val email: String? = null,
    val role: String,
    val status: String,
    @SerialName("must_change_password") val mustChangePassword: Boolean = false,
    @SerialName("branch_id") val branchId: String? = null
)