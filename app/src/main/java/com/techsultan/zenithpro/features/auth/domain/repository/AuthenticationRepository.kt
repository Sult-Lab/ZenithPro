package com.techsultan.zenithpro.features.auth.domain.repository

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.auth.data.remote.SignInRequest
import com.techsultan.zenithpro.features.auth.data.remote.SignInResponseDto
import com.techsultan.zenithpro.features.auth.data.remote.SignUpRequest
import kotlinx.coroutines.flow.Flow

interface AuthenticationRepository {

    fun signUp(request: SignUpRequest): Flow<Resource<Unit>>
    fun login(request: SignInRequest): Flow<Resource<Boolean>>
    fun isUserLoggedIn(): Boolean
    fun logout(): Flow<Resource<Unit>>
    val sessionState: Flow<Boolean>
}
