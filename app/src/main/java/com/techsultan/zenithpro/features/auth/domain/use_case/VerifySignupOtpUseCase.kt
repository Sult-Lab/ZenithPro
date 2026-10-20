package com.techsultan.zenithpro.features.auth.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import kotlinx.coroutines.flow.Flow

class VerifySignupOtpUseCase(
    private val authRepository: AuthenticationRepository
) {
    operator fun invoke(
        email: String,
        token: String
    ): Flow<Resource<Unit>> {
        return authRepository.verifySignupOtp(email, token)
    }
}