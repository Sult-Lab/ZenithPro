package com.techsultan.zenithpro.features.auth.presentation

data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val data: String? = null
)