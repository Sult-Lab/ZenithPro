package com.techsultan.zenithpro.features.settings.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.ImageUploadManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.settings.data.remote.UpdateBusinessRequest
import com.techsultan.zenithpro.features.settings.data.remote.UpdateBusinessResponse
import com.techsultan.zenithpro.features.settings.domain.repository.BusinessRepository
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.UploadData
import io.ktor.client.call.body
import io.ktor.http.ContentType
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import androidx.core.graphics.scale

class BusinessRepositoryImpl(
    private val functions: Functions,
    private val imageUploadManager: ImageUploadManager,
    private val sessionManager: SessionManager,
    private val storage: Storage,
    private val context: Context
) : BusinessRepository {

    override suspend fun updateBusiness(
        request: UpdateBusinessRequest,
        logoUri: Uri?
    ): Resource<UpdateBusinessResponse> = withContext(Dispatchers.IO) {
        try {
            // Upload logo if a new one was picked
            val logoUrl: String? = if (logoUri != null) {
                uploadLogo(logoUri, sessionManager.businessId)
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

    private suspend fun uploadLogo(uri: Uri, businessId: String): String {
        val bytes = compressLogo(uri)
        val fileName = "logo_${businessId}.jpg"
        val path = "business-logos/$businessId/$fileName"

        storage.from("business-assets").upload(
            path = path,
            data = UploadData(
                stream = ByteReadChannel(bytes),
                size = bytes.size.toLong()
            ),
            options = {
                contentType = ContentType.Image.JPEG
                upsert = true
            }
        )

        return storage.from("business-assets").publicUrl(path)
    }

    private fun compressLogo(uri: Uri): ByteArray {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open logo URI")

        val bitmap  = input.use { BitmapFactory.decodeStream(it) }
            ?: throw IOException("Failed to decode logo")

        // Logos are square — resize to 512×512
        val size    = 512
        val scaled  = bitmap.scale(size, size)
        bitmap.recycle()

        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
            scaled.recycle()
            out.toByteArray()
        }
    }
}