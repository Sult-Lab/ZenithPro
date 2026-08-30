package com.techsultan.zenithpro.core.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.data.UserSession
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.manager.ZenithFcmTokenManager
import com.techsultan.zenithpro.core.util.AuthState
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import com.techsultan.zenithpro.features.auth.domain.use_case.LogoutUseCase
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class DataPersistentViewModel(
    private val authRepository: AuthenticationRepository,
    private val auth: Auth,
    private val sessionManager: SessionManager,
    private val logoutUseCase: LogoutUseCase,
    private val postgrest: Postgrest
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
        observeSessionFlow()
    }

    private fun observeSessionFlow() {
        viewModelScope.launch {
            sessionManager.sessionFlow.collect { session ->
                val currentAuth = _authState.value
                if (session != null && currentAuth != AuthState.Loading) {
                    val supabaseSession = auth.currentSessionOrNull()
                    // Only update authState if the session belongs to the current Supabase user
                    if (session.userId == supabaseSession?.user?.id) {
                        if (session.mustChangePassword) {
                            if (currentAuth != AuthState.MustChangePassword) {
                                _authState.value = AuthState.MustChangePassword
                            }
                        } else if (currentAuth == AuthState.MustChangePassword || currentAuth == AuthState.Unauthenticated) {
                            _authState.value = AuthState.Authenticated
                        }
                    }
                }
            }
        }
    }

    fun observeSession() {
        viewModelScope.launch {
            // 1. Quick check for local session to dismiss splash screen immediately if possible
            val immediateSession = sessionManager.loadSession()
            if (immediateSession != null) {
                Log.d("DataPersistentViewModel", "Immediate local session found")
                _authState.value = if (immediateSession.mustChangePassword) {
                    AuthState.MustChangePassword
                } else {
                    AuthState.Authenticated
                }
            }

            try {
                // 2. Wait for Supabase to initialize (with timeout)
                withTimeoutOrNull(3000) {
                    auth.awaitInitialization()
                }

                // 3. Observe auth status for changes
                authRepository.sessionState.collect { isAuthenticated ->
                    Log.d("DataPersistentViewModel", "Auth state changed: isAuthenticated=$isAuthenticated")

                    if (isAuthenticated) {
                        val supabaseSession = auth.currentSessionOrNull()
                        val localSession = sessionManager.loadSession()
                        val supabaseUserId = supabaseSession?.user?.id

                        if (localSession != null && localSession.userId == supabaseUserId) {
                            Log.d("DataPersistentViewModel", "Valid session found: mustChangePassword=${localSession.mustChangePassword}")
                            _authState.value = if (localSession.mustChangePassword) {
                                AuthState.MustChangePassword
                            } else {
                                AuthState.Authenticated
                            }
                            
                            // Background refresh to ensure session data is up to date
                            sessionManager.initSessionFromServer(supabaseUserId)
                        } else {
                            // Supabase authenticated but local data missing, mismatch, or stale
                            Log.d("DataPersistentViewModel", "Session mismatch or missing, initializing from server")
                            if (supabaseUserId != null) {
                                val result = sessionManager.initSessionFromServer(supabaseUserId)

                                if (result.isSuccess) {
                                    val session = result.getOrNull()
                                    _authState.value = if (session?.mustChangePassword == true) {
                                        AuthState.MustChangePassword
                                    } else {
                                        AuthState.Authenticated
                                    }
                                } else {
                                    _authState.value = AuthState.Unauthenticated
                                }
                            } else {
                                _authState.value = AuthState.Unauthenticated
                            }
                        }
                    } else {
                        Log.d("DataPersistentViewModel", "User is unauthenticated")
                        _authState.value = AuthState.Unauthenticated
                    }
                }
            } catch (e: Exception) {
                Log.e("DataPersistentViewModel", "observeSession failed: ${e.message}")
                if (_authState.value == AuthState.Loading) {
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            try {
                ZenithFcmTokenManager.unregisterToken(
                    postgrest = postgrest,
                    terminalId = session.value?.terminalId ?: "",
                    businessId = session.value?.businessId ?: ""
                )
                logoutUseCase().collect()
                // Force final state
                _authState.value = AuthState.Unauthenticated

            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

}
