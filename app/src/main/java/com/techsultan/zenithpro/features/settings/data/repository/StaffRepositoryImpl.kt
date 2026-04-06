package com.techsultan.zenithpro.features.settings.data.repository

import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.settings.data.local.BusinessSettingsDao
import com.techsultan.zenithpro.features.settings.data.local.BusinessSettingsEntity
import com.techsultan.zenithpro.features.settings.data.mapper.toEntity
import com.techsultan.zenithpro.features.settings.data.remote.BusinessSettingsDto
import com.techsultan.zenithpro.features.settings.data.remote.StaffMember
import com.techsultan.zenithpro.features.settings.domain.repository.SettingsRepository
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class SettingsRepositoryImpl(
    private val settingsDao: BusinessSettingsDao,
    private val postgrest: Postgrest,
    private val networkMonitor: NetworkMonitor,
) : SettingsRepository {

    override fun observeSettings(businessId: String) =
        settingsDao.observeSettings(businessId)

    override suspend fun getSettings(businessId: String) =
        settingsDao.getSettings(businessId)

    override suspend fun updateSettings(
        settings: BusinessSettingsEntity
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            settingsDao.insertSettings(settings)
            if (networkMonitor.isConnected()) {
                postgrest.from("business_settings").upsert(
                    BusinessSettingsDto(
                        businessId = settings.businessId,
                        currencySymbol = settings.currencySymbol,
                        currencyCode = settings.currencyCode,
                        taxRate = settings.taxRate,
                        allowNegativeStock = settings.allowNegativeStock,
                        requireCustomerSale = settings.requireCustomerSale,
                        lowStockThreshold = settings.lowStockThreshold,
                        receiptFooter = settings.receiptFooter,
                        updatedAt = Instant.now().toString()
                    )
                ) { onConflict = "business_id" }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save settings")
        }
    }

    override suspend fun updateUserRole(
        userId: String, newRole: String, businessId: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest.from("user_profiles")
                .update(mapOf("role" to newRole)) {
                    filter { eq("id", userId); eq("business_id", businessId) }
                }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update role")
        }
    }

    override suspend fun updateUserStatus(
        userId: String, status: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest.from("user_profiles")
                .update(mapOf("status" to status)) {
                    filter { eq("id", userId) }
                }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update status")
        }
    }

    override suspend fun getStaffList(businessId: String): Resource<List<StaffMember>> =
        withContext(Dispatchers.IO) {
            try {
                val staff = postgrest
                    .rpc("get_staff_list")
                    .decodeList<StaffMember>()
                Resource.Success(staff)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to load staff")
            }
        }

    override suspend fun pullFromServer(businessId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val remote = postgrest.from("business_settings")
                    .select { filter { eq("business_id", businessId) } }
                    .decodeSingleOrNull<BusinessSettingsDto>()
                remote?.let { settingsDao.insertSettings(it.toEntity()) }
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Pull failed")
            }
        }
}