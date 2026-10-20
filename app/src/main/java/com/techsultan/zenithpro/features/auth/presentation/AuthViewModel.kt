package com.techsultan.zenithpro.features.auth.presentation

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.AnalyticsHelper
import androidx.core.os.bundleOf
import com.techsultan.zenithpro.features.auth.data.remote.SignInRequest
import com.techsultan.zenithpro.features.auth.data.remote.SignUpRequest
import com.techsultan.zenithpro.features.settings.domain.use_case.CreateStaffUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.IsUserLoggedInUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.LoginUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.LogoutUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.ResendConfirmationEmailUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.SignUpUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.VerifySignupOtpUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AuthViewModel(
    private val signUpUseCase: SignUpUseCase,
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase,
    private val createStaffUseCase: CreateStaffUseCase,
    private val analytics: AnalyticsHelper,
    private val verifySignupOtpUseCase: VerifySignupOtpUseCase,
    private val resendConfirmationEmailUseCase: ResendConfirmationEmailUseCase
) : ViewModel() {

    private val _signUpState = mutableStateOf(AuthState())
    val signUpState: State<AuthState> = _signUpState

    private val _loginState = mutableStateOf(AuthState())
    val loginState: State<AuthState> = _loginState

    private val _createStaffState = mutableStateOf(AuthState())
    val createStaffState: State<AuthState> = _createStaffState

    private val _events = MutableSharedFlow<AuthEvent>()
    val events = _events.asSharedFlow()

    fun signUp(request: SignUpRequest, logoUri: Uri?) {
        analytics.trackEvent("registration_started")
        signUpUseCase(request, logoUri).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    val normalizedEmail = request.email.trim().lowercase()
                    _signUpState.value = _signUpState.value.copy(
                        isLoading = false,
                        registrationComplete = true,
                        email = normalizedEmail,
                        isSuccess = true
                    )
                    analytics.trackEvent("registration_success", bundleOf(
                        "business_name" to request.businessName
                    ))
                    _events.emit(AuthEvent.SignUpSuccess)
                }
                is Resource.Error -> {
                    val errorCode = result.message ?: "unknown"
                    val errorMessage = when {
                        errorCode.contains("429") -> "Too many requests. Please try again later."
                        errorCode.contains("422") || errorCode.contains("already registered") ->
                            "This email is already registered. Please log in."
                        else -> errorCode
                    }
                    _signUpState.value = _signUpState.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                    analytics.trackEvent("registration_failure", bundleOf(
                        "error_code" to errorCode
                    ))
                }
                is Resource.Loading -> {
                    _signUpState.value = _signUpState.value.copy(isLoading = true)
                }
            }
        }.launchIn(viewModelScope)
    }

    fun login(request: SignInRequest) {
        loginUseCase(request).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    _loginState.value = AuthState(isSuccess = true)
                    // Note: login_success with role/business_id is tracked in SessionManager.initSessionFromServer
                    val event = if (result.data == true) AuthEvent.LoginSuccessMustChangePassword
                    else
                        AuthEvent.LoginSuccess
                    _events.emit(event)
                }
                is Resource.Error -> {
                    val errorCode = result.message ?: "unknown"
                    if (errorCode.contains("Email not confirmed", ignoreCase = true)) {
                        _loginState.value = AuthState(
                            error = "Please confirm your email before signing in.",
                            showResendOption = true,
                            unconfirmedEmail = request.email
                        )
                    } else {
                        _loginState.value = AuthState(error = errorCode)
                    }
                    analytics.trackEvent("login_failure", bundleOf(
                        "error_code" to errorCode
                    ))
                }
                is Resource.Loading -> {
                    _loginState.value = AuthState(isLoading = true)
                }
            }
        }.launchIn(viewModelScope)
    }

    private var verifyOtpJob: Job? = null
    private var resendOtpJob: Job? = null


    fun onOtpChanged(code: String) {
        _signUpState.value = _signUpState.value.copy(
            otpCode = code,
            otpError = null,
            error = null
        )
    }

    private var verificationJob: Job? = null

    fun verifySignupOtp(email: String, token: String) {
        val code = token.trim()

        if (code.length != 6 || !code.all(Char::isDigit)) {
            _signUpState.value = _signUpState.value.copy(
                otpError = "Enter the complete 6-digit code."
            )
            return
        }

        if (_signUpState.value.isVerifyingEmail) return

        verificationJob?.cancel()
        verificationJob = verifySignupOtpUseCase(
            email.trim().lowercase(),
            token = code.trim()
        ).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _signUpState.value = _signUpState.value.copy(
                        isVerifyingEmail = true,
                        otpError = null
                    )
                }

                is Resource.Success -> {
                    _signUpState.value = _signUpState.value.copy(
                        isVerifyingEmail = false,
                        isEmailVerified = true,
                        otpCode = "",
                        otpError = null
                    )
                    _events.emit(AuthEvent.EmailVerificationSuccess)
                }

                is Resource.Error -> {
                    _signUpState.value = _signUpState.value.copy(
                        isVerifyingEmail = false,
                        otpError = result.message?.trim()
                            ?: "Verification failed. Please try again."
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun resendSignupOtp() {
        val email = _signUpState.value.email
            ?.trim()
            ?.lowercase()
            .orEmpty()

        if (email.isBlank() || _signUpState.value.isResendingOtp) return

        viewModelScope.launch {
            _signUpState.value = _signUpState.value.copy(
                isResendingOtp = true,
                otpError = null,
                resendOtpMessage = null
            )

            try {
                when (val result =
                    resendConfirmationEmailUseCase(email)) {
                    is Resource.Success -> {
                        _signUpState.value = _signUpState.value.copy(
                            isResendingOtp = false,
                            otpCode = "",
                            resendOtpMessage =
                                "A new verification code has been sent."
                        )
                    }

                    is Resource.Error -> {
                        _signUpState.value = _signUpState.value.copy(
                            isResendingOtp = false,
                            otpError = result.message
                                ?: "Failed to resend the code."
                        )
                    }

                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _signUpState.value = _signUpState.value.copy(
                    isResendingOtp = false,
                    otpError = "Failed to resend the code. Please try again."
                )
            }
        }
    }

    fun logout() {
        logoutUseCase().launchIn(viewModelScope)
    }

    fun resetSignUpState() {
        _signUpState.value = AuthState()
    }

    fun isUserLoggedIn(): Boolean {
        return isUserLoggedInUseCase()
    }

    sealed class AuthEvent {
        object LoginSuccess : AuthEvent()
        object SignUpSuccess : AuthEvent()
        object LoginSuccessMustChangePassword : AuthEvent()

        object EmailVerificationSuccess : AuthEvent()
    }
}
