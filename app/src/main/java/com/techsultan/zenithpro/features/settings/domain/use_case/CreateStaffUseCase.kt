package com.techsultan.zenithpro.features.settings.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import com.techsultan.zenithpro.features.settings.data.remote.CreateStaffRequest
import com.techsultan.zenithpro.features.settings.data.remote.CreateStaffResponse
import com.techsultan.zenithpro.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class CreateStaffUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(
        request: CreateStaffRequest
    ): Resource<CreateStaffResponse> {
        if (request.email.isBlank())
            return Resource.Error("Email is required")
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(request.email).matches())
            return Resource.Error("Enter a valid email address")
        if (request.password.length < 6)
            return Resource.Error("Password must be at least 6 characters")
        if (request.firstName.isBlank())
            return Resource.Error("First name is required")
        if (request.role !in listOf("ADMIN", "MANAGER", "STAFF"))
            return Resource.Error("Invalid role")
        return repository.createStaff(request)
    }
}