package com.techsultan.zenithpro.core.manager

import android.util.Log
import com.techsultan.zenithpro.core.data.UserSession
import com.techsultan.zenithpro.core.data.local.SessionDataStore
import com.techsultan.zenithpro.core.data.remote.BusinessDto
import com.techsultan.zenithpro.core.data.remote.UserProfileDto
import com.techsultan.zenithpro.core.util.BusinessLogoManager
import com.techsultan.zenithpro.features.branch.data.remote.BranchDto
import com.techsultan.zenithpro.features.settings.data.remote.BusinessSettingsDto
import com.techsultan.zenithpro.features.settings.data.local.TerminalDao
import com.techsultan.zenithpro.features.settings.data.mapper.toEntity
import com.techsultan.zenithpro.features.settings.data.remote.TerminalDto
import com.techsultan.zenithpro.features.settings.data.remote.UpdateBusinessResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.util.Objects.isNull

class SessionManager(
    private val sessionDataStore: SessionDataStore,
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val logoManager: BusinessLogoManager,
    private val terminalDao: TerminalDao
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
            Log.d("SessionManager", "Loaded profile: $profile")

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

            val settings = runCatching {
                withContext(Dispatchers.IO) {
                    postgrest
                        .from("business_settings")
                        .select { filter { eq("business_id", profile.businessId) } }
                        .decodeSingleOrNull<BusinessSettingsDto>()
                }
            }.getOrNull()

            val remote = postgrest.from("terminals").select {
                filter { eq("business_id", profile.businessId) }  // ← use profile.businessId, not businessId
            }.decodeList<TerminalDto>()

            if (remote.isNotEmpty()) {
                terminalDao.insertTerminals(remote.map { it.toEntity() })
            }

            // Fetch all active branches for this business
            val branches = withContext(Dispatchers.IO) {
                postgrest
                    .from("branches")
                    .select {
                        filter {
                            eq("business_id", profile.businessId)
                            isNull("deleted_at")
                            eq("is_active", true)
                        }
                    }
                    .decodeList<BranchDto>()
            }
            Log.d("SessionManager", "Fetched ${branches.size} branches")

            // Branch resolution — only admins have null branchId
            // For staff: use their assigned branch, or auto-resolve if only one branch exists
            val resolvedBranchId: String?
            val resolvedBranchName: String?

            when {
                profile.role == "ADMIN" -> {
                    // Admin sees all branches — no fixed branch
                    resolvedBranchId   = null
                    resolvedBranchName = null
                }
                profile.branchId != null -> {
                    // Staff explicitly assigned to a branch
                    val branch = branches.firstOrNull { it.id == profile.branchId }
                    resolvedBranchId   = branch?.id ?: profile.branchId
                    resolvedBranchName = branch?.name
                    Log.d("SessionManager", "Staff assigned to branch: $resolvedBranchName")
                }
                branches.size == 1 -> {
                    // Staff not assigned but only one branch — auto-assign
                    resolvedBranchId   = branches.first().id
                    resolvedBranchName = branches.first().name
                    Log.d("SessionManager", "Auto-resolved single branch: $resolvedBranchName")
                }
                else -> {
                    // Multiple branches, staff not assigned — they'll pick at checkout
                    resolvedBranchId   = null
                    resolvedBranchName = null
                    Log.w("SessionManager", "Staff has no branch and multiple branches exist")
                }
            }
            // Replace the terminal resolution block with this:
            val terminal: TerminalDto? = when {
                // ADMIN — take first active terminal for the business
                profile.role == "ADMIN" -> {
                    remote.firstOrNull { it.isActive }
                }
                // Staff — find terminal matching their resolved branch
                resolvedBranchId != null -> {
                    remote.firstOrNull {
                        it.branchId == resolvedBranchId && it.isActive
                    }
                }
                else -> null
            }

            Log.d("SessionManager", "Resolved terminal: ${terminal?.id} " +
                    "name=${terminal?.name} for branch=$resolvedBranchName")
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
                mustChangePassword = profile.mustChangePassword,
                currencySymbol  = settings?.currencySymbol ?: "₦",
                currencyCode    = business.currencyCode.ifBlank { settings?.currencyCode ?: "NGN" },
                branchId        = resolvedBranchId,
                branchName      = resolvedBranchName,
                businessType    = business.type,
                businessEmail   = business.email,
                businessLogoUrl = business.logoUrl,
                terminalId      = terminal?.id
            )

            logoManager.loadLogo(session.businessLogoUrl)
            sessionDataStore.saveSession(session)
            _currentSession = session
            Log.d("SessionManager", "Session initialized: ${session.fullName} branch=${session.branchName}")

            ZenithFcmTokenManager.registerTokenForTerminal(
                postgrest  = postgrest,
                terminalId = terminal?.id ?: "",
                businessId = profile.businessId,
            )
            Log.d("SessionManager", "FCM token registered for terminal ${terminal?.id}")
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
            currencyCode    = response.currencyCode,
            terminalId      = current.terminalId
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
