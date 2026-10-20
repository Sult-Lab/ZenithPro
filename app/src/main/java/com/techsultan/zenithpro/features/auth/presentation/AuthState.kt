package com.techsultan.zenithpro.features.auth.presentation


data class AuthState(
    // General request state
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val data: String? = null,

    // Registration state
    val registrationComplete: Boolean = false,
    val email: String? = null,

    // Existing login / confirmation state
    val showResendOption: Boolean = false,
    val unconfirmedEmail: String? = null,

    // Email OTP verification state
    val otpCode: String = "",
    val isVerifyingEmail: Boolean = false,
    val isEmailVerified: Boolean = false,
    val otpError: String? = null,

    // Resend OTP state
    val isResendingOtp: Boolean = false,
    val resendOtpMessage: String? = null
)