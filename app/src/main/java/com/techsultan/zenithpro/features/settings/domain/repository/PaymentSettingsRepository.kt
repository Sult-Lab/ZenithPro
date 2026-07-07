package com.techsultan.zenithpro.features.settings.domain.repository

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.settings.data.local.TerminalEntity
import kotlinx.coroutines.flow.Flow

interface PaymentSettingsRepository {

    fun getTerminals(businessId: String): Flow<Resource<List<TerminalEntity>>>

    suspend fun updateSweepAccount(
        terminalId: String,
        bankCode: String,
        accountNumber: String,
        accountName: String
    ): Resource<Unit>

    suspend fun pullFromServer(businessId: String): Resource<Unit>

}