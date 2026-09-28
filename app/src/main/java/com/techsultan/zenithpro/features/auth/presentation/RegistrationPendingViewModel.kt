package com.techsultan.zenithpro.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegistrationPendingViewModel(
    private val authRepository: AuthenticationRepository
) : ViewModel() {

    private val _resendCooldown = MutableStateFlow(0)
    val resendCooldown = _resendCooldown.asStateFlow()
    val canResend: Boolean get() = _resendCooldown.value == 0

    private val _resendStatus = MutableStateFlow<Resource<Unit>?>(null)
    val resendStatus = _resendStatus.asStateFlow()

    fun resendEmail(email: String) {
        if (!canResend) return
        
        viewModelScope.launch {
            _resendStatus.value = Resource.Loading()
            val result = authRepository.resendConfirmationEmail(email)
            _resendStatus.value = result
            
            if (result is Resource.Success) {
                // Start 60 second cooldown
                _resendCooldown.value = 60
                while (_resendCooldown.value > 0) {
                    delay(1000)
                    _resendCooldown.update { it - 1 }
                }
            }
        }
    }

    fun resetStatus() {
        _resendStatus.value = null
    }
}
