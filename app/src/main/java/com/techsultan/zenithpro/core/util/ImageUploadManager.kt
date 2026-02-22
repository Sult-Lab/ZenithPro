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
import io.github.jan.supabase.storage.storage
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class ImageUploadManager(
    private val supabaseClient: SupabaseClient,
    private val context: Context,
) {
    private val storage = supabaseClient.storage
    private val functions = supabaseClient.functions

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
        businessId: String
    ): List<String> = coroutineScope {

        imageUris.map { uri ->
            async(Dispatchers.IO) {

                val compressedBytes = compressAndResizeImage(uri)
                    ?: throw Exception("Image compression failed")

                uploadSingleImage(compressedBytes, businessId)
            }
        }.awaitAll()
    }

    private suspend fun uploadSingleImage(
        imageBytes: ByteArray,
        businessId: String
    ): String {
        val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        Log.d("ImageUpload", "Uploading image, size: ${imageBytes.size}, base64 length: ${base64.length}")

        val request = ImageUploadRequest(
            imageBase64 = base64,
            businessId = businessId
        )

        val response = functions.invoke(
            function = "upload_product_image",
            body = request
        )

        val bodyText = response.bodyAsText()
        Log.d("ImageUpload", "Response status: ${response.status}, body: $bodyText")

        if (!response.status.isSuccess()) {
            throw Exception("Image upload failed: $bodyText")
        }

        return JSONObject(bodyText).getString("imagePath")
    }

    private fun compressAndResizeImage(
        uri: Uri,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024,
        quality: Int = 70
    ): ByteArray? {
        return try {

            // First decode with bounds only (no memory allocation)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            //  Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(
                options,
                maxWidth,
                maxHeight
            )

            //  Decode actual bitmap with sampling
            options.inJustDecodeBounds = false

            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            //  Compress
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            bitmap.recycle()

            outputStream.toByteArray()

        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }


    private fun buildImageUrl(bucketName: String, fileName: String) =
        "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$bucketName/$fileName".replace(" ", "%20")
}
