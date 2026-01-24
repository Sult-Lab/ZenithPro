package com.techsultan.zenithpro.features.auth.domain.repository

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.auth.data.remote.SignInRequest
import com.techsultan.zenithpro.features.auth.data.remote.SignUpRequest
import kotlinx.coroutines.flow.Flow

interface AuthenticationRepository {

    fun signUp(request: SignUpRequest): Flow<Resource<Unit>>
    fun login(request: SignInRequest): Flow<Resource<Unit>>
    fun isUserLoggedIn(): Boolean
    fun logout(): Flow<Resource<Unit>>
    fun createStaff(
        email: String,
        firstName: String,
        lastName: String,
        role: String,
        temporaryPassword: String
    ): Flow<Resource<String>>
    val sessionState: Flow<Boolean>
}
