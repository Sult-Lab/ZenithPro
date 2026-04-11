package com.techsultan.zenithpro.features.settings.data.repository

import android.util.Log
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.settings.data.local.BusinessSettingsDao
import com.techsultan.zenithpro.features.settings.data.local.BusinessSettingsEntity
import com.techsultan.zenithpro.features.settings.data.mapper.toDto
import com.techsultan.zenithpro.features.settings.data.mapper.toEntity
import com.techsultan.zenithpro.features.settings.data.remote.BusinessSettingsDto
import com.techsultan.zenithpro.features.settings.data.remote.CreateStaffRequest
import com.techsultan.zenithpro.features.settings.data.remote.CreateStaffResponse
import com.techsultan.zenithpro.features.settings.data.remote.StaffMember
import com.techsultan.zenithpro.features.settings.domain.repository.SettingsRepository
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant

class SettingsRepositoryImpl(
    private val settingsDao: BusinessSettingsDao,
    private val postgrest: Postgrest,
    private val networkMonitor: NetworkMonitor,
    private val functions: Functions,
) : SettingsRepository {

    override fun observeSettings(businessId: String): Flow<BusinessSettingsEntity?> =
        settingsDao.observeSettings(businessId)
            .map { settings ->
                settings ?: defaultBusinessSettings(businessId)
            }

    override suspend fun getSettings(businessId: String) =
        settingsDao.getSettings(businessId)

    override suspend fun updateSettings(
        settings: BusinessSettingsEntity
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            settingsDao.insertSettings(settings)
            if (networkMonitor.isConnected()) {
                postgrest.from("business_settings").upsert(
                    settings.toDto().copy(updatedAt = Instant.now().toString())
                ) { onConflict = "business_id" }
            }
            Log.d("SettingsRepository", "Settings updated successfully")
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
                    .from("user_profiles")
                    .select {
                        filter {
                            eq("business_id", businessId)
                            neq("status", "DELETED")
                        }
                    }
                    .decodeList<StaffMember>()
                Resource.Success(staff)
            } catch (e: Exception) {
                Log.e("SettingsRepo", "getStaffList error: ${e.message}", e)
                Resource.Error(e.message ?: "Failed to load staff")
            }
        }

    override suspend fun pullFromServer(businessId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val remote = postgrest
                    .from("business_settings")
                    .select { filter { eq("business_id", businessId) } }
                    .decodeSingleOrNull<BusinessSettingsDto>()

                if (remote != null) {
                    // Server has settings — persist them
                    settingsDao.insertSettings(remote.toEntity())
                    Log.d("SettingsRepo", "Settings pulled: $remote")
                } else {
                    // No server row yet — check if we already have local defaults
                    val existing = settingsDao.getSettings(businessId)
                    if (existing == null) {
                        // Insert defaults locally so the screen never shows null
                        val defaults = defaultBusinessSettings(businessId)
                        settingsDao.insertSettings(defaults)
                        Log.d("SettingsRepo", "No remote settings — inserted defaults")

                        // Also push defaults to server so future pulls find them
                        if (networkMonitor.isConnected()) {
                            postgrest.from("business_settings")
                                .upsert(defaults.toDto()) { onConflict = "business_id" }
                            Log.d("SettingsRepo", "Default settings pushed to server")
                        }
                    } else {
                        Log.d("SettingsRepo", "Using existing local settings")
                    }
                }

                Resource.Success(Unit)
            } catch (e: Exception) {
                Log.e("SettingsRepo", "pullFromServer error: ${e.message}", e)
                Resource.Error(e.message ?: "Pull failed")
            }
        }

    override suspend fun createStaff(
        request: CreateStaffRequest
    ): Resource<CreateStaffResponse> = withContext(Dispatchers.IO) {
        try {
            val response = functions.invoke(
                function = "create_staff",
                body     = request
            )
            val result = response.body<CreateStaffResponse>()
            Resource.Success(result)
        } catch (e: Exception) {
            Log.e("SettingsRepo", "createStaff: ${e.message}", e)
            Resource.Error(
                when {
                    e.message?.contains("already exists") == true ->
                        "A user with this email already exists"
                    e.message?.contains("Managers cannot") == true ->
                        "Managers cannot create admin accounts"
                    else -> e.message ?: "Failed to create staff"
                }
            )
        }
    }

    override suspend fun updateStaffBranch(
        userId: String, branchId: String?
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest.from("user_profiles")
                .update(mapOf("branch_id" to branchId)) {
                    filter { eq("id", userId) }
                }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update branch")
        }
    }

    override suspend fun removeStaff(userId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                postgrest.from("user_profiles")
                    .update(mapOf("status" to "INACTIVE")) {
                        filter { eq("id", userId) }
                    }
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to remove staff")
            }
        }

    fun defaultBusinessSettings(businessId: String) = BusinessSettingsEntity(
        businessId          = businessId,
        currencySymbol      = "₦",
        currencyCode        = "NGN",
        taxRate             = 0.0,
        allowNegativeStock  = false,
        requireCustomerSale = false,
        lowStockThreshold   = 5,
        receiptFooter       = null,
        updatedAt           = Instant.now().toString()
    )
}
