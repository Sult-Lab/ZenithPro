package com.techsultan.zenithpro.features.settings.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.settings.data.local.BusinessSettingsEntity
import com.techsultan.zenithpro.features.settings.domain.repository.SettingsRepository

class UpdateStaffRoleUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(
        userId: String, newRole: String, businessId: String
    ): Resource<Unit> {
        if (newRole !in listOf("ADMIN", "MANAGER", "STAFF")) {
            return Resource.Error("Invalid role")
        }
        return repository.updateUserRole(userId, newRole, businessId)
    }
}