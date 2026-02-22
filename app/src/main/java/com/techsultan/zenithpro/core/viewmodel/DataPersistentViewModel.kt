package com.techsultan.zenithpro.core.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import com.techsultan.zenithpro.features.auth.domain.use_case.IsUserLoggedInUseCase
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataPersistentViewModel(
    private val authRepository: AuthenticationRepository,
    private val auth: Auth
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn.asStateFlow()

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            auth.awaitInitialization()

            authRepository.sessionState.collect { isAuthenticated ->
                Log.d("DataPersistentViewModel", "Session state changed: $isAuthenticated")
                _isLoggedIn.value = isAuthenticated
                _isLoading.value = false
            }
        }
    }
}