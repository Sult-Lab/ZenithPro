package com.techsultan.zenithpro.features.auth.presentation

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.AnalyticsHelper
import androidx.core.os.bundleOf
import com.techsultan.zenithpro.features.auth.data.remote.SignInRequest
import com.techsultan.zenithpro.features.auth.data.remote.SignUpRequest
import com.techsultan.zenithpro.features.settings.domain.use_case.CreateStaffUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.IsUserLoggedInUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.LoginUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.LogoutUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.SignUpUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AuthViewModel(
    private val signUpUseCase: SignUpUseCase,
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase,
    private val createStaffUseCase: CreateStaffUseCase,
    private val analytics: AnalyticsHelper
) : ViewModel() {

    private val _signUpState = mutableStateOf(AuthState())
    val signUpState: State<AuthState> = _signUpState

    private val _loginState = mutableStateOf(AuthState())
    val loginState: State<AuthState> = _loginState

    private val _createStaffState = mutableStateOf(AuthState())
    val createStaffState: State<AuthState> = _createStaffState

    private val _events = MutableSharedFlow<AuthEvent>()
    val events = _events.asSharedFlow()

    fun signUp(request: SignUpRequest, logoUri: Uri?) {
        analytics.trackEvent("registration_started")
        signUpUseCase(request, logoUri).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    val normalizedEmail = request.email.trim().lowercase()
                    _signUpState.value = _signUpState.value.copy(
                        isLoading = false,
                        registrationComplete = true,
                        email = normalizedEmail,
                        isSuccess = true
                    )
                    analytics.trackEvent("registration_success", bundleOf(
                        "business_name" to request.businessName
                    ))
                    _events.emit(AuthEvent.SignUpSuccess)
                }
                is Resource.Error -> {
                    val errorCode = result.message ?: "unknown"
                    val errorMessage = when {
                        errorCode.contains("429") -> "Too many requests. Please try again later."
                        errorCode.contains("422") || errorCode.contains("already registered") ->
                            "This email is already registered. Please log in."
                        else -> errorCode
                    }
                    _signUpState.value = _signUpState.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                    analytics.trackEvent("registration_failure", bundleOf(
                        "error_code" to errorCode
                    ))
                }
                is Resource.Loading -> {
                    _signUpState.value = _signUpState.value.copy(isLoading = true)
                }
            }
        }.launchIn(viewModelScope)
    }

    fun login(request: SignInRequest) {
        loginUseCase(request).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    _loginState.value = AuthState(isSuccess = true)
                    // Note: login_success with role/business_id is tracked in SessionManager.initSessionFromServer
                    val event = if (result.data == true) AuthEvent.LoginSuccessMustChangePassword
                    else
                        AuthEvent.LoginSuccess
                    _events.emit(event)
                }
                is Resource.Error -> {
                    val errorCode = result.message ?: "unknown"
                    _loginState.value = AuthState(error = errorCode)
                    analytics.trackEvent("login_failure", bundleOf(
                        "error_code" to errorCode
                    ))
                }
                is Resource.Loading -> {
                    _loginState.value = AuthState(isLoading = true)
                }
            }
        }.launchIn(viewModelScope)
    }

    fun logout() {
        logoutUseCase().launchIn(viewModelScope)
    }

    fun isUserLoggedIn(): Boolean {
        return isUserLoggedInUseCase()
    }

    sealed class AuthEvent {
        object LoginSuccess : AuthEvent()
        object SignUpSuccess : AuthEvent()
        object LoginSuccessMustChangePassword : AuthEvent()
    }
}
