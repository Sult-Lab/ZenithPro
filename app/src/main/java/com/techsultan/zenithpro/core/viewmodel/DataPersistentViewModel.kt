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
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.ZenithAnalytics
import androidx.core.os.bundleOf
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
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
    private val logoutUseCase: LogoutUseCase,
    private val postgrest: Postgrest,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private var hasTrackedColdStart = false

    val session: StateFlow<UserSession?> = sessionManager.sessionFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = sessionManager.currentSession
        )

    init {
        observeSession()
        observeNetwork()
    }

    private var offlineStartTime: Long? = null

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isConnectedFlow.collect { isConnected ->
                ZenithAnalytics.setKey("is_online", isConnected.toString())
                if (isConnected) {
                    offlineStartTime?.let { start ->
                        val duration = (System.currentTimeMillis() - start) / 60000 // minutes
                        ZenithAnalytics.trackEvent("online_mode_restored", bundleOf(
                            "offline_duration_minutes" to duration
                        ))
                        offlineStartTime = null
                    }
                } else {
                    offlineStartTime = System.currentTimeMillis()
                    ZenithAnalytics.trackEvent("offline_mode_entered")
                }
            }
        }
    }

    fun observeSession() {
        viewModelScope.launch {

            auth.awaitInitialization()

            authRepository.sessionState.collect { isAuthenticated ->

                if (isAuthenticated) {
                    val localSession = sessionManager.loadSession()

                    if (localSession != null) {
                        _authState.value = AuthState.Authenticated
                        if (!hasTrackedColdStart) {
                            ZenithAnalytics.trackEvent("app_cold_start", bundleOf(
                                "session_restored" to true
                            ))
                            hasTrackedColdStart = true
                        }
                        
                        // Background refresh to ensure session data is up to date
                        val supabaseSession = auth.currentSessionOrNull()
                        if (supabaseSession != null) {
                            sessionManager.initSessionFromServer(supabaseSession.user?.id ?: "")
                        }
                    } else {
                        val supabaseSession = auth.currentSessionOrNull()

                        if (supabaseSession != null) {
                            val result = sessionManager.initSessionFromServer(
                                supabaseSession.user?.id ?: ""
                            )

                            if (!hasTrackedColdStart) {
                                ZenithAnalytics.trackEvent("app_cold_start", bundleOf(
                                    "session_restored" to true
                                ))
                                hasTrackedColdStart = true
                            }

                            _authState.value = if (result.isSuccess) {
                                AuthState.Authenticated
                            } else {
                                AuthState.Unauthenticated
                            }
                        } else {
                            _authState.value = AuthState.Unauthenticated
                            if (!hasTrackedColdStart) {
                                ZenithAnalytics.trackEvent("app_cold_start", bundleOf(
                                    "session_restored" to false
                                ))
                                hasTrackedColdStart = true
                            }
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
