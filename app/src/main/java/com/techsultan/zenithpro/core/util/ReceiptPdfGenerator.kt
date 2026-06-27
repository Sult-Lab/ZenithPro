package com.techsultan.zenithpro.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.core.content.FileProvider
import com.techsultan.zenithpro.core.data.local.ReceiptData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ReceiptPdfGenerator(
    private val context: Context,
    private val renderer: ReceiptRenderer
) {

    suspend fun generatePdf(
        receipt: ReceiptData
    ): File = withContext(Dispatchers.IO) {

        val width = 1080
        val height = renderer.calculateHeight(receipt)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = document.startPage(pageInfo)

        page.canvas.drawColor(Color.WHITE)

        renderer.drawReceipt(
            canvas = page.canvas,
            receipt = receipt
        )

        document.finishPage(page)

        val file = File(
            context.cacheDir,
            "receipt_${receipt.receiptNumber}.pdf"
        )

        FileOutputStream(file).use { outputStream ->
            document.writeTo(outputStream)
        }

        document.close()

        file
    }
}
