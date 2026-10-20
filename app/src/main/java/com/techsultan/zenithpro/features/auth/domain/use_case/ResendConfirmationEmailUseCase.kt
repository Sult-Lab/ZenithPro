package com.techsultan.zenithpro.features.auth.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import kotlinx.coroutines.flow.Flow

class ResendConfirmationEmailUseCase(
    private val authRepository: AuthenticationRepository
) {
    suspend operator fun invoke(
        email: String
    ): Resource<Unit> {
        if (email.isBlank()) {
            return Resource.Error("Email address is required.")
        }

        return authRepository.resendConfirmationEmail(
            email = email.trim().lowercase()
        )
    }
}