package com.techsultan.zenithpro.core.util

import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.util.Util.formatPrice

class ReceiptFormatter {

    fun format(
        receipt: ReceiptData
    ): String {

        val itemsText = receipt.items.joinToString("\n") { item ->
            "[L]${item.name}\n[L]${item.qty} x NGN ${item.price.formatPrice()}[R] NGN ${item.total.formatPrice()}"
        }

        val splitText = if (receipt.splitPayments.isNotEmpty()) {
            receipt.splitPayments.joinToString("\n") {
                "[L]${it.method}[R] NGN ${it.amount.formatPrice()}"
            }
        } else {
            ""
        }

        val customerLine = if (!receipt.customerName.isNullOrBlank()) {
            "[L]Customer:[R]${receipt.customerName}\n"
        } else {
            ""
        }

        val discountLine = if (receipt.discount > 0) {
            "[L]Discount:[R] NGN ${receipt.discount.formatPrice()}\n"
        } else {
            ""
        }

        val subtotalLine = if (receipt.discount > 0 && receipt.subtotal > 0) {
            "[L]Subtotal:[R] NGN ${receipt.subtotal.formatPrice()}\n"
        } else {
            ""
        }

        val paidLine = if (receipt.amountPaid > 0) {
            "[L]Paid:[R] NGN ${receipt.amountPaid.formatPrice()}\n"
        } else {
            ""
        }

        val changeLine = if (receipt.change > 0) {
            "[L]Change:[R] NGN ${receipt.change.formatPrice()}\n"
        } else {
            ""
        }

        val splitSection = if (splitText.isNotBlank()) {
            "[C]------------------------------\n$splitText\n"
        } else {
            ""
        }

        return """
[C]<font size='big'><b>${receipt.businessName}</b></font>
[C]------------------------------
[L]Receipt No:[R]${receipt.receiptNumber}
[L]Cashier:[R]${receipt.cashierName}
[L]Payment:[R]${receipt.paymentMethod}
$customerLine[C]------------------------------
$itemsText
[C]------------------------------
$subtotalLine$discountLine[L]<b>TOTAL:</b>[R]<b>NGN ${receipt.total.formatPrice()}</b>
$splitSection[C]------------------------------
$paidLine$changeLine[C]------------------------------
[C]${receipt.businessName}
[C]${receipt.businessAddress}
[C]${receipt.businessNumber}
[C]Thanks for your patronage
        """.trimIndent()
    }
}
