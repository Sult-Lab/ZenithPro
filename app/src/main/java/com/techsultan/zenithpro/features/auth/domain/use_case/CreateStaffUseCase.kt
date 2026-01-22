package com.techsultan.zenithpro.features.auth.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import kotlinx.coroutines.flow.Flow

class CreateStaffUseCase(private val repository: AuthenticationRepository) {

    operator fun invoke(
        email: String,
        firstName: String,
        lastName: String,
        role: String,
        temporaryPassword: String
    ): Flow<Resource<String>> {
        return repository.createStaff(email, firstName, lastName, role, temporaryPassword)
    }
}