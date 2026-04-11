package com.techsultan.zenithpro.features.auth.data.mapper

import com.techsultan.zenithpro.core.data.UserSession
import com.techsultan.zenithpro.features.auth.data.remote.SignInResponseDto

fun SignInResponseDto.toUserSession() = UserSession(
    userId = user.id,
    businessId = business.id,
    firstName = user.firstName,
    lastName = user.lastName,
    email = user.email,
    role = user.role,
    businessName = business.name,
    businessPhone = business.phone,
    businessAddress = business.address,
    currencySymbol = settings.currencySymbol,
    mustChangePassword = user.mustChangePassword
)