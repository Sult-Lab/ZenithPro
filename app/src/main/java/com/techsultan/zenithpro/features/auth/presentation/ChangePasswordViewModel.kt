package com.techsultan.zenithpro.features.auth.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.ZenithAnalytics
import androidx.core.os.bundleOf
import com.techsultan.zenithpro.core.navigation.Route
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.functions.Functions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChangePasswordViewModel(
    private val auth: Auth,
    private val functions: Functions,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ChangePasswordEvent>()
    val events = _events.asSharedFlow()

    fun init(mode: Route.PasswordChangeMode, email: String?) {
        _state.update { it.copy(mode = mode, email = email ?: "") }
    }

    fun onEmailChanged(value: String) {
        _state.update { it.copy(email = value, emailError = null) }
    }

    fun onOtpChanged(value: String) {
        _state.update { it.copy(otpCode = value, otpError = null) }
    }

    fun onPasswordChanged(value: String) {
        _state.update { it.copy(password = value, passwordError = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _state.update { it.copy(confirmPassword = value, confirmError = null) }
    }

    fun submit() {
        val s = _state.value
        
        if (s.mode == Route.PasswordChangeMode.FORGOT) {
            if (!s.isOtpSent) {
                submitForgotPassword(s.email)
            } else if (!s.isOtpVerified) {
                verifyOtpCode(s.email, s.otpCode)
            } else {
                submitChangePassword(s)
            }
        } else {
            submitChangePassword(s)
        }
    }

    fun submitForgotPassword(email: String) {
        if (email.isBlank()) {
            _state.update { it.copy(emailError = "Email is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                auth.resetPasswordForEmail(email = email)
                _state.update { it.copy(isLoading = false, isOtpSent = true, resendCountdown = 60) }
                _events.emit(ChangePasswordEvent.ResetLinkSent)
                startCountdown()
            } catch (e: Exception) {
                Log.e("ChangePasswordVM", "forgotPassword: ${e.message}", e)
                ZenithAnalytics.logError(e, context = "ChangePasswordViewModel.submitForgotPassword")
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun verifyOtpCode(email: String, code: String) {
        if (code.length < 6) {
            _state.update { it.copy(otpError = "Enter 6-digit code") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                auth.verifyEmailOtp(
                    type = OtpType.Email.RECOVERY,
                    email = email,
                    token = code
                )
                _state.update { it.copy(isLoading = false, isOtpVerified = true) }
            } catch (e: Exception) {
                Log.e("ChangePasswordVM", "verifyOtpCode: ${e.message}", e)
                _state.update { it.copy(isLoading = false, error = e.message ?: "Invalid code") }
            }
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            while (_state.value.resendCountdown > 0) {
                kotlinx.coroutines.delay(1000L)
                _state.update { it.copy(resendCountdown = it.resendCountdown - 1) }
            }
        }
    }

    private fun submitChangePassword(s: ChangePasswordUiState) {
        var hasError = false

        if (s.password.length < 6) {
            _state.update { it.copy(passwordError = "Minimum 6 characters") }
            hasError = true
        }
        if (s.password != s.confirmPassword) {
            _state.update { it.copy(confirmError = "Passwords do not match") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                if (s.mode == Route.PasswordChangeMode.FORGOT) {
                    // When recovering password via OTP/Link, use standard Googe Play/Supabase Auth updateUser API
                    auth.updateUser {
                        password = s.password
                    }
                } else {
                    // For typical authenticated password updates or administrator forced changes, invoke function
                    functions.invoke(
                        function = "change_password",
                        body = mapOf("newPassword" to s.password)
                    )
                }

                sessionManager.currentSession?.let { session ->
                    sessionManager.saveSession(
                        session.copy(mustChangePassword = false)
                    )
                }
                ZenithAnalytics.trackEvent("password_changed", bundleOf(
                    "is_forced_change" to (s.mode == Route.PasswordChangeMode.FORCED)
                ))
                _state.update { it.copy(isLoading = false) }
                _events.emit(ChangePasswordEvent.Changed)
            } catch (e: Exception) {
                Log.e("ChangePasswordVM", "submit: ${e.message}", e)
                ZenithAnalytics.logError(e, context = "ChangePasswordViewModel.submitChangePassword")
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    sealed class ChangePasswordEvent {
        data object Changed : ChangePasswordEvent()
        data object ResetLinkSent : ChangePasswordEvent()
    }
}

data class ChangePasswordUiState(
    val mode: Route.PasswordChangeMode = Route.PasswordChangeMode.CHANGE,
    val email: String                  = "",
    val otpCode: String                = "",
    val password: String               = "",
    val confirmPassword: String        = "",
    val isLoading: Boolean             = false,
    val isOtpSent: Boolean             = false,
    val isOtpVerified: Boolean         = false,
    val emailError: String?            = null,
    val otpError: String?              = null,
    val passwordError: String?         = null,
    val confirmError: String?          = null,
    val error: String?                 = null,
    val resendCountdown: Int           = 0
)
