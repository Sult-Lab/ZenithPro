package com.techsultan.zenithpro.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.scale

object Util {
    fun convertLongToFullDate(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(date)
    }

    fun Context.compressImageFromUri(
        uri: Uri,
        quality: Int = 70 // 60–80 is usually the sweet spot
    ): ByteArray? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    ?: return null

                val outputStream = ByteArrayOutputStream()

                originalBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    quality,
                    outputStream
                )

                originalBitmap.recycle()

                outputStream.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun Context.compressAndResizeImage(
        uri: Uri,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024,
        quality: Int = 70
    ): ByteArray? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    ?: return null

                val resizedBitmap = originalBitmap.scale(maxWidth, maxHeight)

                val outputStream = ByteArrayOutputStream()

                resizedBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    quality,
                    outputStream
                )

                originalBitmap.recycle()
                resizedBitmap.recycle()

                outputStream.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    enum class SyncStatus {
        SYNCED,      // In sync with server
        PENDING,     // Awaiting first sync
        DIRTY,       // Modified locally after last sync
        DELETED      // Soft-deleted, pending server delete
    }

    fun Long.formatPrice(): String = String.format("%,.0f", this.toDouble())

    fun String.trimOrNull(): String? = trim().ifBlank { null }

}
