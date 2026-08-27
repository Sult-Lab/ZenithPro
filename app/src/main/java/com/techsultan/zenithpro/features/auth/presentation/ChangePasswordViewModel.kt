package com.techsultan.zenithpro.features.auth.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.ZenithAnalytics
import androidx.core.os.bundleOf
import com.techsultan.zenithpro.core.navigation.Route
import io.github.jan.supabase.auth.Auth
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

    fun onPasswordChanged(value: String) {
        _state.update { it.copy(password = value, passwordError = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _state.update { it.copy(confirmPassword = value, confirmError = null) }
    }

    fun submit() {
        val s = _state.value
        
        if (s.mode == Route.PasswordChangeMode.FORGOT) {
            submitForgotPassword(s.email)
        } else {
            submitChangePassword(s)
        }
    }

    private fun submitForgotPassword(email: String) {
        if (email.isBlank()) {
            _state.update { it.copy(emailError = "Email is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                auth.resetPasswordForEmail(email)
                _state.update { it.copy(isLoading = false) }
                _events.emit(ChangePasswordEvent.ResetLinkSent)
            } catch (e: Exception) {
                Log.e("ChangePasswordVM", "forgotPassword: ${e.message}", e)
                ZenithAnalytics.logError(e, context = "ChangePasswordViewModel.submitForgotPassword")
                _state.update { it.copy(isLoading = false, error = e.message) }
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
                functions.invoke(
                    function = "change_password",
                    body = mapOf("newPassword" to s.password)
                )

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
    val password: String               = "",
    val confirmPassword: String        = "",
    val isLoading: Boolean             = false,
    val emailError: String?            = null,
    val passwordError: String?         = null,
    val confirmError: String?          = null,
    val error: String?                 = null
)
