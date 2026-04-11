package com.techsultan.zenithpro.core.di

import com.techsultan.zenithpro.core.data.local.SessionDataStore
import com.techsultan.zenithpro.core.manager.SessionManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val sessionModule = module {
    single { SessionDataStore(androidContext()) }
    single {
        SessionManager(
            sessionDataStore = get(),
            postgrest = get(),
            auth = get()
        )
    }
}