package com.techsultan.zenithpro.features.auth.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import kotlinx.coroutines.flow.Flow

class LogoutUseCase(private val repository: AuthenticationRepository) {

    operator fun invoke(): Flow<Resource<Unit>> {
        return repository.logout()
    }
}