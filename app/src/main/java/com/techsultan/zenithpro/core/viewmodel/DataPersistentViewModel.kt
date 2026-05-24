package com.techsultan.zenithpro.core.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.data.UserSession
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.AuthState
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import com.techsultan.zenithpro.features.auth.domain.use_case.LogoutUseCase
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DataPersistentViewModel(
    private val authRepository: AuthenticationRepository,
    private val auth: Auth,
    private val sessionManager: SessionManager,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    val session: StateFlow<UserSession?> = sessionManager.sessionFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = sessionManager.currentSession
        )

    init {
        observeSession()
    }

    fun observeSession() {
        viewModelScope.launch {

            auth.awaitInitialization()

            authRepository.sessionState.collect { isAuthenticated ->

                if (isAuthenticated) {
                    val localSession = sessionManager.loadSession()

                    if (localSession != null) {
                        _authState.value = AuthState.Authenticated
                    } else {
                        val supabaseSession = auth.currentSessionOrNull()

                        if (supabaseSession != null) {
                            val result = sessionManager.initSessionFromServer(
                                supabaseSession.user?.id ?: ""
                            )

                            _authState.value = if (result.isSuccess) {
                                AuthState.Authenticated
                            } else {
                                AuthState.Unauthenticated
                            }
                        } else {
                            _authState.value = AuthState.Unauthenticated
                        }
                    }

                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            try {
                logoutUseCase().collect()

                // Force final state
                _authState.value = AuthState.Unauthenticated

            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

}
