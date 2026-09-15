package com.techsultan.zenithpro.features.auth.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class EmailConfirmedViewModel(
    private val authRepository: AuthenticationRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<EmailConfirmState>(EmailConfirmState.Verifying)
    val state: StateFlow<EmailConfirmState> = _state.asStateFlow()

    fun verifyToken(token: String) {
        authRepository.verifyEmail(token).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    _state.value = EmailConfirmState.Success
                }
                is Resource.Error -> {
                    Log.e("EmailConfirm", "Token verification failed: ${result.message}")
                    _state.value = EmailConfirmState.Error(
                        message = when {
                            result.message?.contains("expired", ignoreCase = true) == true ->
                                "This confirmation link has expired. Please register again."
                            result.message?.contains("already confirmed", ignoreCase = true) == true ->
                                "Your email is already confirmed. Please sign in."
                            else ->
                                "Confirmation failed. Please try again or contact support."
                        }
                    )
                }
                is Resource.Loading -> {
                    _state.value = EmailConfirmState.Verifying
                }
            }
        }.launchIn(viewModelScope)
    }
}

sealed class EmailConfirmState {
    object Verifying : EmailConfirmState()
    object Success   : EmailConfirmState()
    data class Error(val message: String) : EmailConfirmState()
}