package com.techsultan.zenithpro.features.settings.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.settings.data.remote.EditStaffRequest
import com.techsultan.zenithpro.features.settings.data.remote.EditStaffResponse
import com.techsultan.zenithpro.features.settings.domain.repository.SettingsRepository

class EditStaffUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(
        request: EditStaffRequest
    ): Resource<EditStaffResponse> {
        if (request.firstName.isBlank())
            return Resource.Error("First name is required")
        if (request.role !in listOf("ADMIN", "MANAGER", "STAFF"))
            return Resource.Error("Invalid role")
        if (request.status !in listOf("ACTIVE", "INACTIVE"))
            return Resource.Error("Invalid status")
        return repository.editStaff(request)
    }
}