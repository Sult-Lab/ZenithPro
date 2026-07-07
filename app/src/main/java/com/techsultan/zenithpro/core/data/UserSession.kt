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
    val currencySymbol: String = "₦",
    val branchId: String? = null,
    val branchName: String? = null,
    val mustChangePassword: Boolean = false,
    val businessType: String?,
    val businessEmail: String?,
    val businessLogoUrl: String?,
    val currencyCode: String   = "NGN",
) {
    val fullName: String
        get() = "$firstName ${lastName.orEmpty()}".trim()

    val isAdmin: Boolean
        get() = role == "ADMIN"

    val isManager: Boolean get() = role in listOf("ADMIN", "MANAGER")
    val staffId: String get() = userId
    val hasBranch: Boolean  get() = branchId != null
    val canSelectBranch: Boolean get() = isAdmin || isManager || !hasBranch
}