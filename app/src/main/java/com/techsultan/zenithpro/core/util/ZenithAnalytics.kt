package com.techsultan.zenithpro.core.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.techsultan.zenithpro.BuildConfig

class ZenithAnalyticsImpl(context: Context) : AnalyticsHelper {
    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    init {
        // Only enable Crashlytics in release builds
        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        setKey("app_version", BuildConfig.VERSION_NAME)
    }

    // Set user context — call after login
    override fun setUser(userId: String, businessId: String, role: String, businessName: String) {
        analytics.setUserId(userId)
        crashlytics.setUserId(userId)
        
        setKey("business_id", businessId)
        setKey("business_name", businessName)
        setKey("user_role", role)
        
        analytics.setUserProperty("business_id", businessId)
        analytics.setUserProperty("user_role", role)
    }

    // Clear user context — call after logout
    override fun clearUser() {
        analytics.setUserId(null)
        crashlytics.setUserId("")
    }

    // Log non-fatal errors — call in catch blocks
    override fun logError(throwable: Throwable, context: String?) {
        context?.let { crashlytics.setCustomKey("error_context", it) }
        crashlytics.recordException(throwable)
    }

    // Log custom key-value pairs to crash reports
    override fun setKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    // Log a breadcrumb message
    override fun log(message: String) {
        crashlytics.log(message)
    }

    // Track screen views
    override fun trackScreen(screenName: String, screenClass: String?) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let { putString(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    // Track events
    override fun trackEvent(name: String, params: Bundle?) {
        analytics.logEvent(name, params)
    }
}
