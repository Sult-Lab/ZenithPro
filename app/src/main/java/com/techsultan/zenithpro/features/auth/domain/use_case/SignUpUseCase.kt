package com.techsultan.zenithpro.features.auth.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.auth.data.remote.SignUpRequest
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import kotlinx.coroutines.flow.Flow

class SignUpUseCase(private val repository: AuthenticationRepository) {

    operator fun invoke(request: SignUpRequest): Flow<Resource<Unit>> {
        return repository.signUp(request)
    }
}