package com.techsultan.zenithpro.core.util

sealed interface AuthState {
    object Loading : AuthState
    object Authenticated : AuthState
    object Unauthenticated : AuthState
    object MustChangePassword : AuthState
}