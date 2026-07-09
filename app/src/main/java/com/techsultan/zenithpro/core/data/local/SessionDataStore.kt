package com.techsultan.zenithpro.core.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.techsultan.zenithpro.core.data.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.collections.get

object SessionKeys {
    val USER_ID  = stringPreferencesKey("user_id")
    val BUSINESS_ID  = stringPreferencesKey("business_id")
    val FIRST_NAME = stringPreferencesKey("first_name")
    val LAST_NAME  = stringPreferencesKey("last_name")
    val EMAIL = stringPreferencesKey("email")
    val ROLE = stringPreferencesKey("role")
    val BUSINESS_NAME = stringPreferencesKey("business_name")
    val BUSINESS_PHONE = stringPreferencesKey("business_phone")
    val BUSINESS_ADDRESS = stringPreferencesKey("business_address")
    val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
    val BRANCH_ID = stringPreferencesKey("branch_id")
    val BRANCH_NAME = stringPreferencesKey("branch_name")
    val BUSINESS_TYPE = stringPreferencesKey("business_type")
    val BUSINESS_EMAIL = stringPreferencesKey("business_email")
    val BUSINESS_LOGO_URL = stringPreferencesKey("business_logo_url")
    val CURRENCY_CODE = stringPreferencesKey("currency_code")
    val TERMINAL_ID = stringPreferencesKey("terminal_id")
}

class SessionDataStore(private val context: Context) {

    private val dataStore = context.createDataStore

    suspend fun saveSession(session: UserSession) {
        try {
            dataStore.edit { prefs ->
                prefs[SessionKeys.USER_ID] = session.userId
                prefs[SessionKeys.BUSINESS_ID] = session.businessId
                prefs[SessionKeys.FIRST_NAME] = session.firstName
                prefs[SessionKeys.LAST_NAME] = session.lastName ?: ""
                prefs[SessionKeys.EMAIL] = session.email ?: ""
                prefs[SessionKeys.ROLE] = session.role
                prefs[SessionKeys.BUSINESS_NAME] = session.businessName
                prefs[SessionKeys.BUSINESS_PHONE] = session.businessPhone ?: ""
                prefs[SessionKeys.BUSINESS_ADDRESS] = session.businessAddress ?: ""
                prefs[SessionKeys.CURRENCY_SYMBOL]  = session.currencySymbol
                prefs[SessionKeys.BRANCH_ID] = session.branchId ?: ""
                prefs[SessionKeys.BRANCH_NAME] = session.branchName ?: ""
                prefs[SessionKeys.BUSINESS_TYPE]     = session.businessType ?: ""
                prefs[SessionKeys.BUSINESS_EMAIL]    = session.businessEmail ?: ""
                prefs[SessionKeys.BUSINESS_LOGO_URL] = session.businessLogoUrl ?: ""
                prefs[SessionKeys.CURRENCY_CODE]     = session.currencyCode
                prefs[SessionKeys.TERMINAL_ID]       = session.terminalId ?: ""
            }
            Log.d("SessionDataStore", "Session saved successfully for user: ${session.userId}")
        } catch (e: Exception) {
            Log.e("SessionDataStore", "Failed to save session: ${e.message}", e)
        }
    }

    suspend fun clearSession() {
        dataStore.edit { it.clear() }
    }

    suspend fun updateCurrencySymbol(symbol: String) {
        dataStore.edit { it[SessionKeys.CURRENCY_SYMBOL] = symbol }
    }

    suspend fun getSession(): UserSession? {
        val prefs  = dataStore.data.first()
        val userId = prefs[SessionKeys.USER_ID]
        val businessId = prefs[SessionKeys.BUSINESS_ID]
        val firstName = prefs[SessionKeys.FIRST_NAME]

        if (userId.isNullOrBlank() || businessId.isNullOrBlank() || firstName.isNullOrBlank()) {
            Log.d("SessionDataStore", "getSession: Missing required keys (userId=$userId, businessId=$businessId, firstName=$firstName)")
            return null
        }

        return UserSession(
            userId = userId,
            businessId = businessId,
            firstName = firstName,
            lastName = prefs[SessionKeys.LAST_NAME]?.ifBlank { null },
            email = prefs[SessionKeys.EMAIL]?.ifBlank { null },
            role = prefs[SessionKeys.ROLE] ?: "STAFF",
            businessName = prefs[SessionKeys.BUSINESS_NAME] ?: "",
            businessPhone = prefs[SessionKeys.BUSINESS_PHONE]?.ifBlank { null },
            businessAddress = prefs[SessionKeys.BUSINESS_ADDRESS]?.ifBlank { null },
            currencySymbol = prefs[SessionKeys.CURRENCY_SYMBOL] ?: "₦",
            branchId = prefs[SessionKeys.BRANCH_ID]?.ifBlank { null },
            branchName = prefs[SessionKeys.BRANCH_NAME]?.ifBlank { null },
            businessType = prefs[SessionKeys.BUSINESS_TYPE]?.ifBlank { null },
            businessEmail = prefs[SessionKeys.BUSINESS_EMAIL]?.ifBlank { null },
            businessLogoUrl = prefs[SessionKeys.BUSINESS_LOGO_URL]?.ifBlank { null },
            currencyCode = prefs[SessionKeys.CURRENCY_CODE] ?: "NGN",
            terminalId = prefs[SessionKeys.TERMINAL_ID]?.ifBlank { null }
        )
    }

    val sessionFlow: Flow<UserSession?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val userId = prefs[SessionKeys.USER_ID]
            val businessId = prefs[SessionKeys.BUSINESS_ID]
            val firstName = prefs[SessionKeys.FIRST_NAME]

            if (userId.isNullOrBlank() || businessId.isNullOrBlank() || firstName.isNullOrBlank()) {
                return@map null
            }

            UserSession(
                userId = userId,
                businessId = businessId,
                firstName = firstName,
                lastName = prefs[SessionKeys.LAST_NAME]?.ifBlank { null },
                email = prefs[SessionKeys.EMAIL]?.ifBlank { null },
                role = prefs[SessionKeys.ROLE] ?: "STAFF",
                businessName = prefs[SessionKeys.BUSINESS_NAME] ?: "",
                businessPhone = prefs[SessionKeys.BUSINESS_PHONE]?.ifBlank { null },
                businessAddress = prefs[SessionKeys.BUSINESS_ADDRESS]?.ifBlank { null },
                currencySymbol = prefs[SessionKeys.CURRENCY_SYMBOL] ?: "₦",
                branchId = prefs[SessionKeys.BRANCH_ID]?.ifBlank { null },
                branchName = prefs[SessionKeys.BRANCH_NAME]?.ifBlank { null },
                businessType = prefs[SessionKeys.BUSINESS_TYPE]?.ifBlank { null },
                businessEmail = prefs[SessionKeys.BUSINESS_EMAIL]?.ifBlank { null },
                businessLogoUrl = prefs[SessionKeys.BUSINESS_LOGO_URL]?.ifBlank { null },
                currencyCode = prefs[SessionKeys.CURRENCY_CODE] ?: "NGN",
                terminalId = prefs[SessionKeys.TERMINAL_ID]?.ifBlank { null }
            )
        }
}

private val Context.createDataStore by preferencesDataStore(name = "session")
