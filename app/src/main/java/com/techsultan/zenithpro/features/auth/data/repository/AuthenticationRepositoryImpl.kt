package com.techsultan.zenithpro.features.auth.data.repository

import android.util.Log
import com.techsultan.zenithpro.core.database.ZenithDatabase
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.settings.data.remote.CreateStaffRequest
import com.techsultan.zenithpro.features.settings.data.remote.CreateStaffResponse
import com.techsultan.zenithpro.features.auth.data.remote.SignInRequest
import com.techsultan.zenithpro.features.auth.data.remote.SignUpRequest
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class AuthenticationRepositoryImpl(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val functions: Functions,
    private val sessionManager: SessionManager,
    private val database: ZenithDatabase
) : AuthenticationRepository {

    override fun signUp(request: SignUpRequest): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            functions.invoke(
                function = "signup",
                body = request
            )
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            Log.e("AuthenticationRepositoryImpl", "signUp: ${e.message}")
            emit(Resource.Error(e.localizedMessage ?: "An error occurred during sign up"))
        }
    }

    override fun login(request: SignInRequest): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            auth.signInWith(Email) {
                email = request.email
                password = request.password
            }
            val session = auth.currentSessionOrNull()
            if (session != null) {
                val result = sessionManager.initSessionFromServer(session.user?.id ?: "")
                result.fold(
                    onSuccess = { userSession ->
                        emit(Resource.Success(userSession.mustChangePassword))
                    },
                    onFailure = { e ->
                        emit(Resource.Error(e.localizedMessage ?: "Failed to load session"))
                    }
                )
            } else {
                emit(Resource.Error("No session found after login"))
            }
        } catch (e: Exception) {
            Log.e("AuthenticationRepositoryImpl", "login: ${e.message}")
            emit(Resource.Error(e.localizedMessage ?: "Login failed"))
        }
    }

    override fun isUserLoggedIn(): Boolean {
        return auth.currentSessionOrNull() != null
    }

    override fun logout(): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            sessionManager.signOut()
            database.clearAllTables()

            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Logout failed"))
        }
    }.flowOn(Dispatchers.IO)

    override val sessionState: Flow<Boolean> = auth.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Authenticated -> true
                is SessionStatus.NotAuthenticated -> false
                is SessionStatus.RefreshFailure -> true
                is SessionStatus.Initializing  -> null // Still loading
            }
        }
        .filterNotNull() // Only emit when we have a definite state
        .distinctUntilChanged()
}
