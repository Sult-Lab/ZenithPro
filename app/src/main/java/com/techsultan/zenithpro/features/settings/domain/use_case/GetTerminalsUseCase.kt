package com.techsultan.zenithpro.features.settings.domain.use_case

import com.techsultan.zenithpro.features.settings.domain.repository.PaymentSettingsRepository

class GetTerminalsUseCase(
    private val repository: PaymentSettingsRepository
) {
    operator fun invoke(businessId: String) = repository.getTerminals(businessId)
}
