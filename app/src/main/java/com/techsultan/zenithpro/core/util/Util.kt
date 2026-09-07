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
import com.techsultan.zenithpro.core.domain.domain.UnitType
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object Util {
    fun convertLongToFullDate(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(date)
    }

    fun Long.formatDateTime(): String {
        val date = Date(this)
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
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

    fun formatQuantity(quantity: Double, unitType: UnitType): String {
        return if (unitType.isDecimal) {
            if (quantity == quantity.toLong().toDouble()) {
                quantity.toLong().toString()   // show "2" not "2.0"
            } else {
                "%.3f".format(quantity).trimEnd('0').trimEnd('.')
            }
        } else {
            quantity.toLong().toString()
        }
    }

    fun formatReceiptQuantity(quantity: Double, unitType: UnitType): String {
        return "${formatQuantity(quantity, unitType)} ${unitType.abbreviation}"
        // → "2.5 kg", "3 pcs", "0.5 bag", "1 L"
    }

    fun formatStockDisplay(quantity: Double, unitType: UnitType): String {
        return "${formatQuantity(quantity, unitType)} ${unitType.abbreviation} in stock"
        // → "50 pcs in stock", "12.5 kg in stock"
    }

    fun formatRelativeTime(isoTime: String): String = runCatching {
        val then = Instant.parse(isoTime)
        val now = Instant.now()
        val diffSeconds = ChronoUnit.SECONDS.between(then, now)
        val diffMinutes = ChronoUnit.MINUTES.between(then, now)
        val diffHours = ChronoUnit.HOURS.between(then, now)
        val diffDays = ChronoUnit.DAYS.between(then, now)

        when {
            diffSeconds < 60 -> "just now"
            diffMinutes < 60 -> "$diffMinutes min${if (diffMinutes > 1) "s" else ""} ago"
            diffHours < 24 -> "$diffHours hr${if (diffHours > 1) "s" else ""} ago"
            diffDays < 7 -> "$diffDays day${if (diffDays > 1) "s" else ""} ago"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
                then.atZone(ZoneId.systemDefault()).format(formatter)
            }
        }
    }.getOrElse { "unknown time" }
}
