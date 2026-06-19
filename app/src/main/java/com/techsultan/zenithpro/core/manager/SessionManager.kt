package com.techsultan.zenithpro.core.manager

import android.util.Log
import com.techsultan.zenithpro.core.data.UserSession
import com.techsultan.zenithpro.core.data.local.SessionDataStore
import com.techsultan.zenithpro.core.data.remote.BusinessDto
import com.techsultan.zenithpro.core.data.remote.UserProfileDto
import com.techsultan.zenithpro.core.util.BusinessLogoManager
import com.techsultan.zenithpro.features.branch.data.remote.BranchDto
import com.techsultan.zenithpro.features.settings.data.remote.BusinessSettingsDto
import com.techsultan.zenithpro.features.settings.data.remote.UpdateBusinessResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class SessionManager(
    private val sessionDataStore: SessionDataStore,
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val logoManager: BusinessLogoManager
) {

    @Volatile
    private var _currentSession: UserSession? = null
    val currentSession: UserSession? get() = _currentSession

    val sessionFlow: Flow<UserSession?> = sessionDataStore.sessionFlow
        .onEach { _currentSession = it }


    suspend fun loadSession(): UserSession? {
        // Try memory first
        _currentSession?.let { return it }

        // Try DataStore
        val stored = sessionDataStore.getSession()
        Log.d("SessionManager", "Loaded session from DataStore: $stored")
        if (stored != null) {
            _currentSession = stored
            logoManager.loadLogo(stored.businessLogoUrl)
            return stored
        }

        return null
    }

    suspend fun saveSession(session: UserSession) {
        sessionDataStore.saveSession(session)
        _currentSession = session
    }

    suspend fun initSessionFromServer(userId: String): Result<UserSession> {
        return try {
            val profile = withContext(Dispatchers.IO) {
                postgrest
                    .from("user_profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingle<UserProfileDto>()
            }
            Log.d("SessionManager", "Loaded profile from Supabase: $profile")

            if (profile.status != "ACTIVE") {
                auth.signOut()
                return Result.failure(Exception("Account is inactive"))
            }

            val business = withContext(Dispatchers.IO) {
                postgrest
                    .from("businesses")
                    .select { filter { eq("id", profile.businessId) } }
                    .decodeSingle<BusinessDto>()
            }

            val branchName: String? = profile.branchId?.let { branchId ->
                runCatching {
                    withContext(Dispatchers.IO) {
                        postgrest.from("branches")
                            .select { filter { eq("id", branchId) } }
                            .decodeSingle<BranchDto>()
                            .name
                    }
                }.getOrNull()
            }

            val settings = runCatching {
                withContext(Dispatchers.IO) {
                    postgrest
                        .from("business_settings")
                        .select { filter { eq("business_id", profile.businessId) } }
                        .decodeSingleOrNull<BusinessSettingsDto>()
                }
            }.getOrNull()

            val session = UserSession(
                userId = userId,
                businessId = profile.businessId,
                firstName = profile.firstName,
                lastName = profile.lastName,
                email = profile.email,
                role = profile.role,
                businessName = business.name,
                businessPhone = business.phone,
                businessAddress = business.address,
                mustChangePassword = profile.mustChangePassword,
                currencySymbol = settings?.currencySymbol ?: "₦",
                branchId = if (profile.role == "ADMIN") null else profile.branchId,
                branchName = if (profile.role == "ADMIN") null else branchName,
                businessType    = business.type,
                businessEmail   = business.email,
                businessLogoUrl = business.logoUrl,
                currencyCode    = business.currencyCode
                    .ifBlank { settings?.currencyCode ?: "NGN" },
            )
            Log.d("SessionManager", "Session initialized: $session")
            logoManager.loadLogo(session.businessLogoUrl)
            sessionDataStore.saveSession(session)
            _currentSession = session
            Log.d("SessionManager", "Session initialized and saved for: ${session.fullName}")
            Result.success(session)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("SessionManager", "initSessionFromServer failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateBusinessInSession(response: UpdateBusinessResponse) {
        val current = _currentSession ?: return
        val updated = current.copy(
            businessName    = response.name,
            businessType    = response.type,
            businessPhone   = response.phone,
            businessAddress = response.address,
            businessEmail   = response.email,
            businessLogoUrl = response.logoUrl,
            currencySymbol  = response.currencySymbol,
            currencyCode    = response.currencyCode
        )
        sessionDataStore.saveSession(updated)
        _currentSession = updated
        logoManager.loadLogo(updated.businessLogoUrl)
    }

    suspend fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.w("SessionManager", "Supabase sign out failed: ${e.message}")
        } finally {
            sessionDataStore.clearSession()
            _currentSession = null
            logoManager.clear()
        }
    }

    val userId: String
        get() = _currentSession?.userId
            ?: error("SessionManager: userId accessed before session loaded")

    val businessId: String
        get() = _currentSession?.businessId
            ?: error("SessionManager: businessId accessed before session loaded")

    val isAdmin: Boolean
        get() = _currentSession?.isAdmin ?: false

    val isManager: Boolean
        get() = _currentSession?.isManager ?: false
}
