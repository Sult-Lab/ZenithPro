package com.techsultan.zenithpro.features.auth.domain.use_case

import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository

class IsUserLoggedInUseCase(private val repository: AuthenticationRepository) {

    operator fun invoke(): Boolean {
        return repository.isUserLoggedIn()
    }
}