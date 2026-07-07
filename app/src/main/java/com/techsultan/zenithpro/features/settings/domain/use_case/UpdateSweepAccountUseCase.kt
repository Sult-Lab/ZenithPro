package com.techsultan.zenithpro.features.settings.domain.use_case

import com.techsultan.zenithpro.features.settings.domain.repository.PaymentSettingsRepository

class UpdateSweepAccountUseCase(
    private val repository: PaymentSettingsRepository
) {
    suspend operator fun invoke(
        terminalId: String,
        bankCode: String,
        accountNumber: String,
        accountName: String
    ) = repository.updateSweepAccount(terminalId, bankCode, accountNumber, accountName)
}
