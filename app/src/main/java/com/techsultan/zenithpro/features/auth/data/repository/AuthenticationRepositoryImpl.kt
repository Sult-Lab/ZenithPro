package com.techsultan.zenithpro.features.auth.data.repository

import android.util.Log
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.auth.data.remote.CreateStaffRequest
import com.techsultan.zenithpro.features.auth.data.remote.CreateStaffResponse
import com.techsultan.zenithpro.features.auth.data.remote.SignInRequest
import com.techsultan.zenithpro.features.auth.data.remote.SignUpRequest
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.call.body
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthenticationRepositoryImpl(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val functions: Functions,
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

    override fun login(request: SignInRequest): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            auth.signInWith(Email) {
                email = request.email
                password = request.password
            }
            val session = auth.currentSessionOrNull()
            Log.d("SignIn", "Session after login: ${session?.user?.email}")
            emit(Resource.Success(Unit))
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
            auth.signOut()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Logout failed"))
        }
    }

    override fun createStaff(
        email: String,
        firstName: String,
        lastName: String,
        role: String,
        temporaryPassword: String
    ): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val request = CreateStaffRequest(
                email = email,
                firstName = firstName,
                lastName = lastName,
                role = role,
                temporaryPassword = temporaryPassword
            )
            
            val response = functions.invoke(
                function = "create_staff",
                body = request
            )
            val staffResponse = response.body<CreateStaffResponse>()
            emit(Resource.Success(staffResponse.staffUserId))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to create staff member"))
        }
    }

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
