package com.techsultan.zenithpro.core.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppDataStore(private val context: Context) {

    companion object {
        private val RECEIPT_COUNTER_KEY = intPreferencesKey("receipt_counter")
    }

    val receiptCounter: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[RECEIPT_COUNTER_KEY] ?: 0
    }

    suspend fun saveReceiptCounter(counter: Int) {
        context.dataStore.edit { preferences ->
            preferences[RECEIPT_COUNTER_KEY] = counter
        }
    }
}
