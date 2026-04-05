package com.techsultan.zenithpro.core.manager

import android.util.Log
import com.techsultan.zenithpro.core.data.UserSession
import com.techsultan.zenithpro.core.data.local.SessionDataStore
import com.techsultan.zenithpro.core.data.remote.BusinessDto
import com.techsultan.zenithpro.core.data.remote.UserProfileDto
import com.techsultan.zenithpro.features.settings.data.remote.BusinessSettingsDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

// data/session/SessionManager.kt
class SessionManager(
    private val sessionDataStore: SessionDataStore,
    private val postgrest: Postgrest,
    private val supabaseClient: SupabaseClient,
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
        if (stored != null) {
            _currentSession = stored
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
                    .rpc("get_business_profiles")
                    .decodeList<UserProfileDto>()
                    .firstOrNull { it.id == userId }
                    ?: throw Exception("User profile not found")
            }

            if (profile.status != "ACTIVE") {
                supabaseClient.auth.signOut()
                return Result.failure(Exception("Account is inactive. Contact your administrator."))
            }

            val business = withContext(Dispatchers.IO) {
                postgrest
                    .from("businesses")
                    .select { filter { eq("id", profile.businessId) } }
                    .decodeSingle<BusinessDto>()
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
                userId          = userId,
                businessId      = profile.businessId,
                firstName       = profile.firstName,
                lastName        = profile.lastName,
                email           = profile.email,
                role            = profile.role,
                businessName    = business.name,
                businessPhone   = business.phone,
                businessAddress = business.address,
                currencySymbol  = settings?.currencySymbol ?: "₦"
            )

            sessionDataStore.saveSession(session)
            _currentSession = session

            Log.d("SessionManager", "Session initialized: ${session.fullName} — ${session.role}")
            Result.success(session)
        } catch (e: Exception) {
            Log.e("SessionManager", "initSessionFromServer failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
            Log.w("SessionManager", "Supabase sign out failed: ${e.message}")
        } finally {
            sessionDataStore.clearSession()
            _currentSession = null
        }
    }

    // ── Convenience getters — crash early if called before login ──

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
