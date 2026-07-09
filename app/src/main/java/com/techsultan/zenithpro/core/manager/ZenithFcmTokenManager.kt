package com.techsultan.zenithpro.core.manager

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

object ZenithFcmTokenManager {

    private const val PREFS_NAME = "zenith_fcm"
    private const val KEY_TOKEN  = "fcm_token"
    private const val TAG        = "FCMTokenManager"

    fun saveTokenLocally(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOKEN, token).apply()
        Log.d(TAG, "FCM token saved locally")
    }

    fun getLocalToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
    }

    /**
     * Gets FCM token and pushes it to the terminal row in Supabase.
     * Uses its own IO scope — safe to call from any context.
     */
    fun registerTokenForTerminal(
        postgrest: Postgrest,
        terminalId: String,
        businessId: String,
    ) {
        if (terminalId.isBlank()) {
            Log.w(TAG, "No terminal ID — skipping FCM registration")
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(TAG, "FCM token obtained: ${token.take(20)}...")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        postgrest
                            .from("terminals")
                            .update({
                                set("fcm_token", token)
                                set("updated_at", Instant.now().toString())
                            }) {
                                filter {
                                    eq("id", terminalId)
                                    eq("business_id", businessId)
                                }
                            }
                        Log.d(TAG, "FCM token registered for terminal $terminalId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to register FCM token: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get FCM token: ${e.message}")
            }
    }

    fun unregisterToken(
        postgrest: Postgrest,
        terminalId: String,
        businessId: String,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                postgrest.from("terminals").update({
                    set("fcm_token", null as String?)
                    set("updated_at", Instant.now().toString())
                }) {
                    filter {
                        eq("id", terminalId)
                        eq("business_id", businessId)
                    }
                }
                Log.d(TAG, "FCM token unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister FCM token: ${e.message}")
            }
        }
    }
}