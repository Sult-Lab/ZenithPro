package com.techsultan.zenithpro.core.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.util.Util.formatPrice
import com.techsultan.zenithpro.core.util.Util.formatDateTime
import androidx.core.graphics.toColorInt

class ReceiptRenderer(
    private val logoManager: BusinessLogoManager,
) {

    companion object {
        private val GREEN = "#1F5E37".toColorInt()
        private val DARK = "#2F2F2F".toColorInt()
        private val LIGHT = "#D8D8D8".toColorInt()
    }

    private val titlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GREEN
            textSize = 64f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(
                Typeface.SERIF,
                Typeface.BOLD
            )
        }

    private val bodyPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = DARK
            textSize = 34f
        }

    private val bodyPaintCenter =
        Paint(bodyPaint).apply {
            textAlign = Paint.Align.CENTER
        }

    private val labelPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = DARK
            textSize = 28f
            isFakeBoldText = true
        }

    private val labelPaintEnd =
        Paint(labelPaint).apply {
            textAlign = Paint.Align.RIGHT
        }

    private val valuePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = DARK
            textSize = 40f
            isFakeBoldText = true
        }

    private val valuePaintCenter =
        Paint(valuePaint).apply {
            textAlign = Paint.Align.CENTER
        }

    private val valuePaintEnd =
        Paint(valuePaint).apply {
            textAlign = Paint.Align.RIGHT
        }

    private val totalPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GREEN
            textSize = 72f
            isFakeBoldText = true
        }

    private val totalPaintCenter =
        Paint(totalPaint).apply {
            textAlign = Paint.Align.CENTER
        }

    private val totalPaintEnd =
        Paint(totalPaint).apply {
            textAlign = Paint.Align.RIGHT
        }

    private val dividerPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LIGHT
            strokeWidth = 2f
        }

    fun drawReceipt(
        canvas: Canvas,
        receipt: ReceiptData
    ) {
        val logoBitmap = logoManager.logo.value?.let {
            if (it.config == Bitmap.Config.HARDWARE) {
                it.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                it
            }
        }

        drawWatermark(
            canvas,
            logoBitmap,
            canvas.height
        )

        drawContent(
            canvas,
            receipt,
            logoBitmap
        )
    }

    fun calculateHeight(
        receipt: ReceiptData
    ): Int {

        return 1300 +
                (receipt.items.size * 120)
    }

    private fun drawWatermark(
        canvas: Canvas,
        logo: Bitmap?,
        height: Int
    ) {

        logo ?: return

        val size = 650f

        val left =
            (canvas.width - size) / 2

        val top =
            height * 0.30f

        val paint = Paint().apply {
            alpha = 15
        }

        canvas.drawBitmap(
            logo,
            null,
            RectF(
                left,
                top,
                left + size,
                top + size
            ),
            paint
        )
    }

    private fun drawHeader(
        canvas: Canvas,
        receipt: ReceiptData,
        logo: Bitmap?
    ): Float {

        var y = 40f

        logo?.let {

            val size = 170

            val left =
                (canvas.width - size) / 2

            canvas.drawBitmap(
                it,
                null,
                Rect(
                    left,
                    y.toInt(),
                    left + size,
                    y.toInt() + size
                ),
                null
            )

            y += 230
        }

        canvas.drawText(
            receipt.businessName,
            canvas.width / 2f,
            y,
            titlePaint
        )

        y += 55

        canvas.drawText(
            receipt.businessAddress,
            canvas.width / 2f,
            y,
            bodyPaintCenter
        )

        y += 45

        canvas.drawText(
            receipt.businessNumber,
            canvas.width / 2f,
            y,
            bodyPaintCenter
        )

        y += 80

        canvas.drawLine(
            60f,
            y,
            canvas.width - 60f,
            y,
            dividerPaint
        )

        return y + 80
    }

    private fun drawMetadata(
        canvas: Canvas,
        receipt: ReceiptData,
        startY: Float
    ): Float {

        var y = startY

        canvas.drawText(
            "DATE/TIME",
            60f,
            y,
            labelPaint
        )

        canvas.drawText(
            "RECEIPT ID",
            canvas.width - 60f,
            y,
            labelPaintEnd
        )

        y += 50

        canvas.drawText(
            receipt.createdAt.formatDateTime(),
            60f,
            y,
            valuePaint
        )

        canvas.drawText(
            "#${receipt.receiptNumber}",
            canvas.width - 60f,
            y,
            valuePaintEnd
        )

        y += 70

        canvas.drawLine(
            60f,
            y,
            canvas.width - 60f,
            y,
            dividerPaint
        )

        return y + 90
    }

    private fun drawItems(
        canvas: Canvas,
        receipt: ReceiptData,
        startY: Float
    ): Float {

        var y = startY

        receipt.items.forEach { item ->

            canvas.drawText(
                item.name,
                60f,
                y,
                valuePaint
            )

            canvas.drawText(
                "₦${item.total.formatPrice()}",
                canvas.width - 60f,
                y,
                valuePaintEnd
            )

            y += 40

            canvas.drawText(
                "${item.qty} x ₦${item.price.formatPrice()}",
                60f,
                y,
                bodyPaint
            )

            y += 90
        }

        return y
    }

    private fun drawTotals(
        canvas: Canvas,
        receipt: ReceiptData,
        startY: Float
    ): Float {

        var y = startY

        canvas.drawLine(
            60f,
            y,
            canvas.width - 60f,
            y,
            dividerPaint
        )

        y += 80

        drawAmountRow(
            canvas,
            "Subtotal",
            "₦${receipt.subtotal.formatPrice()}",
            y
        )

        y += 60

        drawAmountRow(
            canvas,
            "VAT",
            "${receipt.taxRate}%",
            y
        )

        y += 120

        canvas.drawText(
            "Total",
            60f,
            y,
            totalPaint
        )

        canvas.drawText(
            "₦${receipt.total.formatPrice()}",
            canvas.width - 60f,
            y,
            totalPaintEnd
        )

        return y + 160
    }

    private fun drawFooter(
        canvas: Canvas,
        startY: Float
    ) {

        var y = startY

        canvas.drawText(
            "✓",
            canvas.width / 2f,
            y,
            totalPaintCenter
        )

        y += 70

        canvas.drawText(
            "Thank you for shopping with us!",
            canvas.width / 2f,
            y,
            valuePaintCenter
        )
    }

    private fun drawContent(
        canvas: Canvas,
        receipt: ReceiptData,
        logo: Bitmap?
    ) {

        var y =
            drawHeader(
                canvas,
                receipt,
                logo
            )

        y =
            drawMetadata(
                canvas,
                receipt,
                y
            )

        y =
            drawItems(
                canvas,
                receipt,
                y
            )

        y =
            drawTotals(
                canvas,
                receipt,
                y
            )

        drawFooter(
            canvas,
            y
        )
    }

    // Add this method to ReceiptRenderer class
    private fun drawAmountRow(
        canvas: Canvas,
        label: String,
        value: String,
        y: Float
    ) {
        canvas.drawText(
            label,
            60f,
            y,
            labelPaint
        )

        canvas.drawText(
            value,
            canvas.width - 60f,
            y,
            valuePaintEnd
        )
    }

}