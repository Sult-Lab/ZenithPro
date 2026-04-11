package com.techsultan.zenithpro.features.auth.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import io.github.jan.supabase.functions.Functions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChangePasswordViewModel(
    private val functions: Functions,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ChangePasswordEvent>()
    val events = _events.asSharedFlow()

    fun onPasswordChanged(value: String) {
        _state.update { it.copy(password = value, passwordError = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _state.update { it.copy(confirmPassword = value, confirmError = null) }
    }

    fun submit() {
        val s = _state.value
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
                _state.update { it.copy(isLoading = false) }
                _events.emit(ChangePasswordEvent.Changed)
            } catch (e: Exception) {
                Log.e("ChangePasswordVM", "submit: ${e.message}", e)
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    sealed class ChangePasswordEvent {
        data object Changed : ChangePasswordEvent()
    }
}

data class ChangePasswordUiState(
    val password: String        = "",
    val confirmPassword: String = "",
    val isLoading: Boolean      = false,
    val passwordError: String?  = null,
    val confirmError: String?   = null,
    val error: String?          = null
)