package com.techsultan.zenithpro.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.techsultan.zenithpro.BuildConfig
import com.techsultan.zenithpro.features.product.data.remote.ImageUploadRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.storage.UploadData
import io.github.jan.supabase.storage.storage
import io.ktor.client.statement.bodyAsText
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID

class ImageUploadManager(
    private val supabaseClient: SupabaseClient,
    private val context: Context,
) {
    private val storage = supabaseClient.storage
    private val functions = supabaseClient.functions

    companion object {
        private const val BUCKET = "product_images"
        private const val MAX_WIDTH = 1024
        private const val MAX_HEIGHT = 1024
        private const val JPEG_QUALITY = 85
    }

    suspend fun createBucket(bucketName: String) {
        try {
            storage.createBucket(bucketName) {
                public = true
                allowedMimeTypes(ContentType.Image.JPEG, ContentType.Image.PNG, ContentType.Image.WEBP)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun uploadImage(
        bucketName: String,
        imageBytes: ByteArray,
        fileName: String
    ): Resource<String> {
        return try {
            storage.from(bucketName).upload(
                path = fileName,
                data = imageBytes
            ) {
                upsert = true
            }
            val imageUrl = buildImageUrl(bucketName, fileName)
            Resource.Success(imageUrl)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    suspend fun uploadProductImages(
        imageUris: List<Uri>,
        businessId: String,
        name: String
    ): List<String> = withContext(Dispatchers.IO) {
        imageUris
            .map { uri ->
                async {
                    try {
                        uploadSingleImage(
                            uri = uri,
                            businessId = businessId,
                            name = name
                        )
                    } catch (e: Exception) {
                        Log.e("ImageUploadManager", "Failed to upload $uri: ${e.message}", e)
                        null
                    }
                }
            }
            .awaitAll()
            .filterNotNull()
    }

    private suspend fun uploadSingleImage(
        uri: Uri,
        businessId: String,
        name: String
    ): String {
        val bytes = compressImage(uri)
        val fileName = "${name}_${UUID.randomUUID()}.jpg"
        val storagePath = "products/$businessId/$fileName"
        Log.d("ImageUploadManager", "Uploading to path: $storagePath (${bytes.size} bytes)")
        storage.from(BUCKET).upload(
            path = storagePath,
            data = UploadData(
                stream = ByteReadChannel(bytes),
                size = bytes.size.toLong()
            ),
            options = {
                contentType = ContentType.Image.JPEG
                upsert = true
            }
        )
        Log.d("ImageUploadManager", "Upload successful")
        // Return public URL, not the storage path
        return storage.from(BUCKET).publicUrl(storagePath)
    }

    private fun compressImage(uri: Uri): ByteArray {
        // Resolve to InputStream — handles both content:// and file:// paths
        val inputStream = when {
            uri.scheme == "content" -> context.contentResolver.openInputStream(uri)
            uri.scheme == "file"    -> File(uri.path!!).inputStream()
            else                    -> File(uri.toString()).takeIf { it.exists() }?.inputStream()
        } ?: throw IOException("Cannot open image: $uri")

        val originalBitmap = inputStream.use { stream ->
            BitmapFactory.decodeStream(stream)
                ?: throw IOException("Failed to decode bitmap from $uri")
        }

        val scaled = scaleBitmap(originalBitmap)
        if (scaled !== originalBitmap) originalBitmap.recycle()

        return ByteArrayOutputStream().use { out ->
            val compressed = scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            scaled.recycle()
            if (!compressed) throw IOException("Bitmap compression returned false for $uri")
            out.toByteArray()
        }
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= MAX_WIDTH && height <= MAX_HEIGHT) return bitmap

        val ratio = minOf(MAX_WIDTH.toFloat() / width, MAX_HEIGHT.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }


    private fun buildImageUrl(bucketName: String, fileName: String) =
        "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$bucketName/$fileName".replace(" ", "%20")
}
