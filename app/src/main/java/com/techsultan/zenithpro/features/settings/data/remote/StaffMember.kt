package com.techsultan.zenithpro.features.settings.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StaffMember(
    val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String? = null,
    val email: String? = null,
    val role: String,
    val status: String,
    val phone: String? = null,
    @SerialName("branch_id") val branchId: String? = null,
    @SerialName("must_change_password") val mustChangePassword: Boolean = false
) {
    val fullName: String get() = "$firstName ${lastName.orEmpty()}".trim()
}