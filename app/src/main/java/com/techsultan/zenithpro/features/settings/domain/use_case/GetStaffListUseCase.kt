package com.techsultan.zenithpro.features.settings.domain.use_case

import com.techsultan.zenithpro.features.settings.data.local.BusinessSettingsEntity
import com.techsultan.zenithpro.features.settings.domain.repository.SettingsRepository

class GetStaffListUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(businessId: String) = repository.getStaffList(businessId)
}