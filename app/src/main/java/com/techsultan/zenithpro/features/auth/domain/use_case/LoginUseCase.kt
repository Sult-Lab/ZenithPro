package com.techsultan.zenithpro.features.auth.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.auth.data.remote.SignInRequest
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import kotlinx.coroutines.flow.Flow

class LoginUseCase(private val repository: AuthenticationRepository) {

    operator fun invoke(request: SignInRequest): Flow<Resource<Unit>> {
        return repository.login(request)
    }
}