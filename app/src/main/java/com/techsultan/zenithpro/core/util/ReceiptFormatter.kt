package com.techsultan.zenithpro.core.util

import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.util.Util.formatPrice

class ReceiptFormatter {

    fun format(
        receipt: ReceiptData
    ): String {

        val itemsText = receipt.items.joinToString("\n") { item ->

            """
[L]${item.name}
[L]${item.qty} x ${item.price.formatPrice()}[R]${item.total.formatPrice()}
            """.trimIndent()
        }

        val splitText = if (receipt.splitPayments.isNotEmpty()) {

            receipt.splitPayments.joinToString("\n") {
                "[L]${it.method}[R]${it.amount.formatPrice()}"
            }

        } else {
            ""
        }

        return """
[C]<font size='big'><b>ZENITH STORE</b></font>

[C]------------------------------

[L]Receipt No:[R]${receipt.receiptNumber}
[L]Cashier:[R]${receipt.cashierName}
[L]Payment:[R]${receipt.paymentMethod}

${receipt.customerName?.let {
            "[L]Customer:[R]$it"
        } ?: ""}

[C]------------------------------

$itemsText

[C]------------------------------

[L]Subtotal:[R]${receipt.subtotal.formatPrice()}
[L]Discount:[R]${receipt.discount.formatPrice()}
[L]<b>TOTAL:</b>[R]<b>${receipt.total.formatPrice()}</b>

${if (splitText.isNotBlank()) {

            """
[C]------------------------------

$splitText
"""

        } else ""}

[C]------------------------------

[L]Paid:[R]${receipt.amountPaid.formatPrice()}
[L]Change:[R]${receipt.change.formatPrice()}

[C]
[C]Thank you for your purchase
[C]
        """.trimIndent()
    }
}