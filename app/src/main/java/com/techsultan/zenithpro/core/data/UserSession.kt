package com.techsultan.zenithpro.core.data

data class UserSession(
    val userId: String,
    val businessId: String,
    val firstName: String,
    val lastName: String?,
    val email: String?,
    val role: String,
    val businessName: String,
    val businessPhone: String?,
    val businessAddress: String?,
    val currencySymbol: String = "₦"
) {
    val fullName: String
        get() = "$firstName ${lastName.orEmpty()}".trim()

    val isAdmin: Boolean
        get() = role == "ADMIN"

    val isManager: Boolean
        get() = role in listOf("ADMIN", "MANAGER")
}