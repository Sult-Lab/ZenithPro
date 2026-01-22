package com.techsultan.zenithpro

import android.app.Application
import com.techsultan.zenithpro.core.di.commonModule
import com.techsultan.zenithpro.core.di.supabaseModule
import com.techsultan.zenithpro.features.auth.di.authModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ZenithApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@ZenithApplication)
            modules(supabaseModule, authModule, commonModule)
        }
    }
}
