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
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _activeBranchId = MutableStateFlow<String?>(null)
    val activeBranchId: StateFlow<String?> = _activeBranchId.asStateFlow()

    private val _activeBranchName = MutableStateFlow<String?>(null)
    val activeBranchName: StateFlow<String?> = _activeBranchName.asStateFlow()


    suspend fun loadSession(): UserSession? {
        // Try memory first
        _currentSession?.let { return it }

        // Try DataStore
        val stored = sessionDataStore.getSession()
        Log.d("SessionManager", "Loaded session from DataStore: $stored")
        if (stored != null) {
            _currentSession = stored
            logoManager.loadLogo(stored.businessLogoUrl)
            initActiveBranch(stored)
            return stored
        }

        return null
    }

    suspend fun saveSession(session: UserSession) {
        sessionDataStore.saveSession(session)
        _currentSession = session
    }

    // Called after login to set the initial active branch
    suspend fun initActiveBranch(session: UserSession) {
        if (!session.isAdmin) {
            // Staff/Manager — lock to their branch
            _activeBranchId.value  = session.branchId
            _activeBranchName.value = session.branchName
        } else {
            // Admin — load persisted selection if any
            val (persistedId, persistedName) = sessionDataStore.getSelectedBranch()
            _activeBranchId.value  = persistedId
            _activeBranchName.value = persistedName
        }
    }

    // Called when admin selects a branch
    suspend fun setActiveBranch(branchId: String?, branchName: String?) {
        _activeBranchId.value  = branchId
        _activeBranchName.value = branchName
        sessionDataStore.updateSelectedBranch(branchId, branchName)
    }

    // Convenience — returns active branch or throws if null
    // Used in checkout where branch is required
    fun requireActiveBranchId(): String = _activeBranchId.value
        ?: error("SessionManager: requireActiveBranchId() called before branch selected")

    suspend fun initSessionFromServer(userId: String): Result<UserSession> = coroutineScope {
        try {
            val profileDeferred = async(Dispatchers.IO) {
                postgrest
                    .from("user_profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingle<UserProfileDto>()
            }

            val profile = profileDeferred.await()
            Log.d("SessionManager", "Loaded profile: $profile")

            if (profile.status != "ACTIVE") {
                auth.signOut()
                return@coroutineScope Result.failure(Exception("Account is inactive"))
            }

            // Run these in parallel once we have the businessId
            val businessDeferred = async(Dispatchers.IO) {
                postgrest
                    .from("businesses")
                    .select { filter { eq("id", profile.businessId) } }
                    .decodeSingle<BusinessDto>()
            }

            val settingsDeferred = async(Dispatchers.IO) {
                runCatching {
                    postgrest
                        .from("business_settings")
                        .select { filter { eq("business_id", profile.businessId) } }
                        .decodeSingleOrNull<BusinessSettingsDto>()
                }.getOrNull()
            }

            val terminalsDeferred = async(Dispatchers.IO) {
                postgrest.from("terminals").select {
                    filter { eq("business_id", profile.businessId) }
                }.decodeList<TerminalDto>()
            }

            val branchesDeferred = async(Dispatchers.IO) {
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

            // Wait for all data
            val business = businessDeferred.await()
            val settings = settingsDeferred.await()
            val remote = terminalsDeferred.await()
            val branches = branchesDeferred.await()

            Log.d("SessionManager", "Fetched ${branches.size} branches and ${remote.size} terminals")

            if (remote.isNotEmpty()) {
                terminalDao.insertTerminals(remote.map { it.toEntity() })
            }

            // Branch resolution
            val resolvedBranchId: String?
            val resolvedBranchName: String?

            when {
                profile.role == "ADMIN" -> {
                    resolvedBranchId = null
                    resolvedBranchName = null
                }
                profile.branchId != null -> {
                    val branch = branches.firstOrNull { it.id == profile.branchId }
                    resolvedBranchId = branch?.id ?: profile.branchId
                    resolvedBranchName = branch?.name
                }
                branches.size == 1 -> {
                    resolvedBranchId = branches.first().id
                    resolvedBranchName = branches.first().name
                }
                else -> {
                    resolvedBranchId = null
                    resolvedBranchName = null
                }
            }

            val terminal: TerminalDto? = when {
                profile.role == "ADMIN" -> remote.firstOrNull { it.isActive }
                resolvedBranchId != null -> remote.firstOrNull { it.branchId == resolvedBranchId && it.isActive }
                else -> null
            }

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
                currencyCode = business.currencyCode.ifBlank { settings?.currencyCode ?: "NGN" },
                branchId = resolvedBranchId,
                branchName = resolvedBranchName,
                businessType = business.type,
                businessEmail = business.email,
                businessLogoUrl = business.logoUrl,
                terminalId = terminal?.id
            )

            logoManager.loadLogo(session.businessLogoUrl)
            sessionDataStore.saveSession(session)
            _currentSession = session
            initActiveBranch(session)

            // Fire and forget FCM registration to avoid blocking main flow
            async {
                try {
                    ZenithFcmTokenManager.registerTokenForTerminal(
                        postgrest = postgrest,
                        terminalId = terminal?.id ?: "",
                        businessId = profile.businessId,
                    )
                } catch (e: Exception) {
                    Log.e("SessionManager", "FCM token registration failed: ${e.message}")
                }
            }

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
            // Sign out globally first
            auth.signOut()
        } catch (e: Exception) {
            Log.w("SessionManager", "Supabase global sign out failed: ${e.message}, forcing local sign out")
            try {
                auth.signOut(scope = SignOutScope.LOCAL)
            } catch (localEx: Exception) {
                Log.e("SessionManager", "Local sign out failed: ${localEx.message}")
            }
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
