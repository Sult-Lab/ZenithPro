package com.techsultan.zenithpro.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.techsultan.zenithpro.core.data.local.ReceiptData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.createBitmap

class ReceiptImageGenerator(
    private val context: Context,
    private val renderer: ReceiptRenderer
) {

    suspend fun generateImage(
        receipt: ReceiptData
    ): File = withContext(Dispatchers.IO) {

        val width = 1080
        val height = renderer.calculateHeight(receipt)

        val bitmap = createBitmap(width, height)

        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        renderer.drawReceipt(
            canvas = canvas,
            receipt = receipt
        )

        val file = File(
            context.cacheDir,
            "receipt_${receipt.receiptNumber}.jpg"
        )

        FileOutputStream(file).use { outputStream ->
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                95,
                outputStream
            )
        }

        bitmap.recycle()

        file
    }
}
