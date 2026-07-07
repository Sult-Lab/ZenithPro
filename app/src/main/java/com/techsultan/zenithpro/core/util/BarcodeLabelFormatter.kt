package com.techsultan.zenithpro.core.util

import android.graphics.Bitmap
import android.graphics.Color
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.google.zxing.BarcodeFormat
import com.google.zxing.oned.Code128Writer
import com.techsultan.zenithpro.core.util.Util.formatPrice
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants

class BarcodeLabelFormatter {

    fun format(productWithVariants: ProductWithVariants): String {
        val product = productWithVariants.product
        val variant = productWithVariants.variants.firstOrNull()?.variant

        val rawBarcode = variant?.barcode?.takeIf { it.isNotBlank() }
            ?: variant?.sku?.takeIf { it.isNotBlank() }
            ?: product.id.take(12)

        val sanitizedBarcode = sanitize(rawBarcode)
        val price = variant?.salesPrice ?: product.baseSalesPrice
        
        // Generate barcode as bitmap to avoid shrinking issues with built-in printer tags
        // 384 pixels is standard for 58mm thermal printers (48mm printable area * 8 dots/mm)
        val barcodeBitmap = generateBarcodeBitmap(sanitizedBarcode, 384, 100)
        
        // Create a dummy printer for the conversion method. Cast null to resolve ambiguity.
        val dummyPrinter = EscPosPrinter(null as DeviceConnection?, 203, 48f, 32)
        val hex = PrinterTextParserImg.bitmapToHexadecimalString(dummyPrinter, barcodeBitmap)

        return """
            |[C]${safe(product.name)}
            |[C]NGN ${price.formatPrice()}
            |[C]<img>$hex</img>
            |[C]$sanitizedBarcode
            |
            |
        """.trimMargin()
    }

    private fun sanitize(value: String): String {
        return value
            .replace(" ", "")
            .replace("-", "")
            .replace("\"", "")
            .replace("'", "")
            .uppercase()
            .take(15)
    }

    private fun safe(value: String): String {
        return value.replace(Regex("[^\\x20-\\x7E]"), "")
    }

    fun generateBarcodeBitmap(
        content: String,
        width: Int,
        height: Int
    ): Bitmap {
        val writer = Code128Writer()
        val matrix = writer.encode(content, BarcodeFormat.CODE_128, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
