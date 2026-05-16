package com.techsultan.zenithpro.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.scale
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

    enum class PrinterType {
        BLUETOOTH,
        USB,
        EMBEDDED
    }

    fun String.toUtcLocalDate(): LocalDate? = runCatching {
        OffsetDateTime.parse(this, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            // Use UTC to avoid device timezone skew in sales reports
            .toLocalDate()
    }.getOrNull()

    fun String.formatAsTime(): String = runCatching {
        OffsetDateTime.parse(this, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("h:mm a"))
    }.getOrElse { "--:-- --" }
    fun isSunmiDevice(): Boolean {
        return Build.MANUFACTURER.contains("SUNMI", true)
    }

    fun Long.formatPrice(): String = String.format("%,.0f", this.toDouble())

    fun String.trimOrNull(): String? = trim().ifBlank { null }

    fun vibrate(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager

            vibratorManager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(
                    150, // duration in milliseconds
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    150,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
    }

    fun generateSku(name: String, category: String = "GEN"): String {
        val cleanName = name.filter { it.isLetterOrDigit() }.padEnd(3, 'X').take(3).uppercase()
        val timestamp = System.currentTimeMillis().toString().takeLast(4)
        return "$category-$cleanName-$timestamp"
    }

}
