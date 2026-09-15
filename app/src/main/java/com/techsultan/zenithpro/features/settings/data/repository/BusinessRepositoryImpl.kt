package com.techsultan.zenithpro.features.settings.data.repository

import android.net.Uri
import android.util.Log
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.ImageUploadManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.settings.data.remote.UpdateBusinessRequest
import com.techsultan.zenithpro.features.settings.data.remote.UpdateBusinessResponse
import com.techsultan.zenithpro.features.settings.domain.repository.BusinessRepository
import io.github.jan.supabase.functions.Functions
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BusinessRepositoryImpl(
    private val functions: Functions,
    private val imageUploadManager: ImageUploadManager,
    private val sessionManager: SessionManager,
) : BusinessRepository {

    override suspend fun updateBusiness(
        request: UpdateBusinessRequest,
        logoUri: Uri?
    ): Resource<UpdateBusinessResponse> = withContext(Dispatchers.IO) {
        try {
            // Upload logo if a new one was picked
            val logoUrl: String? = if (logoUri != null) {
                imageUploadManager.uploadBusinessLogo(
                    uri = logoUri,
                    name = request.name,
                    businessId = sessionManager.businessId
                )
            } else {
                request.logoUrl
            }

            val response = functions.invoke(
                function = "update-business",
                body     = request.copy(logoUrl = logoUrl)
            )

            val result = response.body<UpdateBusinessResponse>()

            // Update session so the rest of the app reflects the changes
            sessionManager.updateBusinessInSession(result)

            Resource.Success(result)
        } catch (e: Exception) {
            Log.e("BusinessRepo", "updateBusiness: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to update business")
        }
    }
}