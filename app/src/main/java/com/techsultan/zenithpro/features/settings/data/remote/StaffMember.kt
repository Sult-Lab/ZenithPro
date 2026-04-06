package com.techsultan.zenithpro.features.settings.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StaffMember(
    val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name")  val lastName: String?,
    val role: String,
    val status: String,
    val email: String?
) {
    val fullName: String get() = "$firstName ${lastName.orEmpty()}".trim()
}