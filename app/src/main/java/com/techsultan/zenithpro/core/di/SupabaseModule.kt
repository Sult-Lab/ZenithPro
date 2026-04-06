package com.techsultan.zenithpro.core.di

import com.techsultan.zenithpro.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import org.koin.dsl.module

val supabaseModule = module {

    single<SupabaseClient> {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Postgrest)

            install(Auth) {
                flowType = FlowType.PKCE
                scheme = "app"
                host = "supabase.com"
                sessionManager = SettingsSessionManager()
                alwaysAutoRefresh = true
            }

            install(Storage)

            install(Functions)
        }
    }

    single<Postgrest> {
        get<SupabaseClient>().postgrest
    }

    single<Auth> {
        get<SupabaseClient>().auth
    }

    single<Storage> {
        get<SupabaseClient>().storage
    }

    single<Functions> {
        get<SupabaseClient>().functions
    }

}