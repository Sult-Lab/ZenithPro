package com.techsultan.zenithpro.core.util

import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.util.Util.formatDateTime
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

        val vatAmount = (receipt.subtotal * (receipt.taxRate / 100)).toLong()
        val vatLine = if (receipt.taxRate > 0) {
            "[L]VAT (${receipt.taxRate}%):[R] NGN ${vatAmount.formatPrice()}\n"
        } else {
            ""
        }

        val subtotalLine = "[L]Subtotal:[R] NGN ${receipt.subtotal.formatPrice()}\n"

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

        val footerLine = if (!receipt.footerMessage.isNullOrBlank()) {
            "[C]${receipt.footerMessage}\n"
        } else {
            ""
        }

        val finalTotal = receipt.subtotal - receipt.discount + vatAmount

        return """
[C]<font size='big'><b>${receipt.businessName}</b></font>
[C]------------------------------
[L]Date:[R]${receipt.printedAt.formatDateTime()}
[L]Receipt No:[R]${receipt.receiptNumber}
[L]Cashier:[R]${receipt.cashierName}
[L]Payment:[R]${receipt.paymentMethod}
$customerLine[C]------------------------------
$itemsText
[C]------------------------------
$subtotalLine$discountLine$vatLine[L]<b>TOTAL:</b>[R]<b>NGN ${finalTotal.formatPrice()}</b>
$splitSection[C]------------------------------
$paidLine$changeLine[C]------------------------------
[C]${receipt.businessName}
[C]${receipt.businessAddress}
[C]${receipt.businessNumber}
$footerLine
[C]<b>THANKS YOU FOR YOUR PATRONAGE</b>


        """.trimIndent()
    }
}
