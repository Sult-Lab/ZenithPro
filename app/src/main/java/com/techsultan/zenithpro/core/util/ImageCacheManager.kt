package com.techsultan.zenithpro.core.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

class ImageCacheManager(private val context: Context) {

    /**
     * Copies a content:// URI into app-private cache immediately after the user picks it.
     * Returns a stable file:// path that survives process death and URI permission revocation.
     */
    suspend fun cacheImage(uri: Uri): String = withContext(Dispatchers.IO) {
        val fileName = "${UUID.randomUUID()}.jpg"
        val cacheFile = File(context.cacheDir, "pending_images/$fileName").also {
            it.parentFile?.mkdirs()
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            cacheFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Cannot open URI: $uri")

        cacheFile.absolutePath  // stable file:// path stored in Room
    }

    fun getCachedFile(path: String): File = File(path)

    fun deleteCachedImage(path: String) {
        File(path).takeIf { it.exists() }?.delete()
    }
}