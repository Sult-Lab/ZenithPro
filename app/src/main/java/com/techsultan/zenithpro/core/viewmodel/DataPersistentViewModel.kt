package com.techsultan.zenithpro.core.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataPersistentViewModel(
    private val authRepository: AuthenticationRepository,
    private val auth: Auth,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            auth.awaitInitialization()

            // Try to load local session first
            val localSession = sessionManager.loadSession()
            
            if (localSession != null) {
                _isLoggedIn.value = true
                _isLoading.value = false
            } else {

                val supabaseSession = auth.currentSessionOrNull()
                if (supabaseSession != null) {
                    val result = sessionManager.initSessionFromServer(supabaseSession.user?.id ?: "")
                    _isLoggedIn.value = result.isSuccess
                } else {
                    _isLoggedIn.value = false
                }
                _isLoading.value = false
            }

            authRepository.sessionState.collect { isAuthenticated ->
                Log.d("DataPersistentViewModel", "Session state changed: $isAuthenticated")
                _isLoggedIn.value = isAuthenticated
            }
        }
    }
}