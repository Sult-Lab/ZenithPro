package com.techsultan.zenithpro.features.settings.data.repository

import android.util.Log
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.settings.data.local.TerminalDao
import com.techsultan.zenithpro.features.settings.data.local.TerminalEntity
import com.techsultan.zenithpro.features.settings.data.mapper.toEntity
import com.techsultan.zenithpro.features.settings.data.remote.TerminalDto
import com.techsultan.zenithpro.features.settings.domain.repository.PaymentSettingsRepository
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.time.Instant


class PaymentSettingsRepositoryImpl(
    private val terminalDao: TerminalDao,
    private val postgrest: Postgrest,
    private val networkMonitor: NetworkMonitor,
) : PaymentSettingsRepository {

    override fun getTerminals(businessId: String) =
        terminalDao.getTerminals(businessId)
            .map<List<TerminalEntity>, Resource<List<TerminalEntity>>> { Resource.Success(it) }
            .catch { emit(Resource.Error(it.message ?: "Failed")) }
            .onStart { emit(Resource.Loading()) }

    override suspend fun updateSweepAccount(
        terminalId: String,
        bankCode: String,
        accountNumber: String,
        accountName: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = Instant.now().toString()
            terminalDao.updateSweepDetails(terminalId, bankCode, accountNumber, accountName, now)
            if (networkMonitor.isConnected()) {
             val update = postgrest.from("terminals")
                    .update(mapOf(
                        "nomba_sweep_bank_code" to bankCode,
                        "nomba_sweep_account_number" to accountNumber,
                        "nomba_sweep_account_name" to accountName
                    )) { filter { eq("id", terminalId) } }
                Log.d("PaymentSettingsRepo", "Update bank account: $update")
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("PaymentSettingsRepo", "Failed to update bank account", e)
            Resource.Error(e.message ?: "Failed to update bank account")
        }
    }

    override suspend fun pullFromServer(businessId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d("PaymentSettingsRepo", "Pull from server")
                val remote = postgrest.from("terminals").select {
                    filter { eq("business_id", businessId) }
                }.decodeList<TerminalDto>()
                Log.d("PaymentSettingsRepo", "Pull from server: $remote")
                if (remote.isNotEmpty()) terminalDao.insertTerminals(remote.map { it.toEntity() })
                Resource.Success(Unit)
            } catch (e: Exception) {
                Log.e("PaymentSettingsRepo", "Pull from server failed", e)
                Resource.Error(e.message ?: "Pull failed")
            }
        }
}