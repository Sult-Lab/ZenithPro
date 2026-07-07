package com.techsultan.zenithpro.features.settings.domain.use_case

import com.techsultan.zenithpro.features.settings.domain.repository.PaymentSettingsRepository

class SyncTerminalsUseCase(
    private val repository: PaymentSettingsRepository
) {
    suspend operator fun invoke(businessId: String) = repository.pullFromServer(businessId)
}
