package com.techsultan.zenithpro.features.settings.domain.use_case

import com.techsultan.zenithpro.features.settings.domain.repository.SettingsRepository

class GetSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke(businessId: String) = repository.observeSettings(businessId)
}