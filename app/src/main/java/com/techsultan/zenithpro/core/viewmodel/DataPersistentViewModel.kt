package com.techsultan.zenithpro.core.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import com.techsultan.zenithpro.features.auth.domain.use_case.IsUserLoggedInUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataPersistentViewModel(
    private val authRepository: AuthenticationRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn.asStateFlow()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            Log.d("DataPersistentViewModel", "Starting session check...")

            authRepository.sessionState.collect { isAuthenticated ->
                Log.d("DataPersistentViewModel", "Session state: $isAuthenticated")
                _isLoggedIn.value = isAuthenticated
                _isLoading.value = false
            }
        }
    }

    fun onLoginSuccess() {
        Log.d("DataPersistentViewModel", "Login success called")
        _isLoggedIn.value = true
    }

    fun onLogout() {
        Log.d("DataPersistentViewModel", "Logout called")
        _isLoggedIn.value = false
    }
}