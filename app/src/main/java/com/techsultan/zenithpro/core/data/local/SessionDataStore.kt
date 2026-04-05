package com.techsultan.zenithpro.core.data.local

import android.content.Context
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
    val USER_ID          = stringPreferencesKey("user_id")
    val BUSINESS_ID      = stringPreferencesKey("business_id")
    val FIRST_NAME       = stringPreferencesKey("first_name")
    val LAST_NAME        = stringPreferencesKey("last_name")
    val EMAIL            = stringPreferencesKey("email")
    val ROLE             = stringPreferencesKey("role")
    val BUSINESS_NAME    = stringPreferencesKey("business_name")
    val BUSINESS_PHONE   = stringPreferencesKey("business_phone")
    val BUSINESS_ADDRESS = stringPreferencesKey("business_address")
    val CURRENCY_SYMBOL  = stringPreferencesKey("currency_symbol")
}

class SessionDataStore(private val context: Context) {

    private val dataStore = context.createDataStore

    suspend fun saveSession(session: UserSession) {
        dataStore.edit { prefs ->
            prefs[SessionKeys.USER_ID]          = session.userId
            prefs[SessionKeys.BUSINESS_ID]      = session.businessId
            prefs[SessionKeys.FIRST_NAME]       = session.firstName
            prefs[SessionKeys.LAST_NAME]        = session.lastName ?: ""
            prefs[SessionKeys.EMAIL]            = session.email ?: ""
            prefs[SessionKeys.ROLE]             = session.role
            prefs[SessionKeys.BUSINESS_NAME]    = session.businessName
            prefs[SessionKeys.BUSINESS_PHONE]   = session.businessPhone ?: ""
            prefs[SessionKeys.BUSINESS_ADDRESS] = session.businessAddress ?: ""
            prefs[SessionKeys.CURRENCY_SYMBOL]  = session.currencySymbol
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
        val userId = prefs[SessionKeys.USER_ID] ?: return null
        return UserSession(
            userId          = userId,
            businessId      = prefs[SessionKeys.BUSINESS_ID] ?: return null,
            firstName       = prefs[SessionKeys.FIRST_NAME] ?: return null,
            lastName        = prefs[SessionKeys.LAST_NAME]?.ifBlank { null },
            email           = prefs[SessionKeys.EMAIL]?.ifBlank { null },
            role            = prefs[SessionKeys.ROLE] ?: "STAFF",
            businessName    = prefs[SessionKeys.BUSINESS_NAME] ?: "",
            businessPhone   = prefs[SessionKeys.BUSINESS_PHONE]?.ifBlank { null },
            businessAddress = prefs[SessionKeys.BUSINESS_ADDRESS]?.ifBlank { null },
            currencySymbol  = prefs[SessionKeys.CURRENCY_SYMBOL] ?: "₦"
        )
    }

    val sessionFlow: Flow<UserSession?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val userId = prefs[SessionKeys.USER_ID] ?: return@map null
            UserSession(
                userId          = userId,
                businessId      = prefs[SessionKeys.BUSINESS_ID]      ?: return@map null,
                firstName       = prefs[SessionKeys.FIRST_NAME]       ?: return@map null,
                lastName        = prefs[SessionKeys.LAST_NAME]?.ifBlank { null },
                email           = prefs[SessionKeys.EMAIL]?.ifBlank { null },
                role            = prefs[SessionKeys.ROLE]             ?: "STAFF",
                businessName    = prefs[SessionKeys.BUSINESS_NAME]    ?: "",
                businessPhone   = prefs[SessionKeys.BUSINESS_PHONE]?.ifBlank { null },
                businessAddress = prefs[SessionKeys.BUSINESS_ADDRESS]?.ifBlank { null },
                currencySymbol  = prefs[SessionKeys.CURRENCY_SYMBOL]  ?: "₦"
            )
        }
}

private val Context.createDataStore by preferencesDataStore(name = "session")