package com.techsultan.zenithpro.core.util

import android.os.Bundle

interface AnalyticsHelper {
    fun setUser(userId: String, businessId: String, role: String, businessName: String)
    fun clearUser()
    fun logError(throwable: Throwable, context: String? = null)
    fun setKey(key: String, value: String)
    fun log(message: String)
    fun trackScreen(screenName: String, screenClass: String? = null)
    fun trackEvent(name: String, params: Bundle? = null)
}
