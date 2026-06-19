package com.techsultan.zenithpro.features.sales.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.util.Util.formatDateTime
import com.techsultan.zenithpro.core.util.Util.formatPrice

@Composable
fun ReceiptCard(
    receipt: ReceiptData
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            if (!receipt.businessLogo.isNullOrBlank()) {
                AsyncImage(
                    model = receipt.businessLogo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(8.dp))
            }

            Text(
                receipt.businessName,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Text(
                receipt.businessAddress,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(
                receipt.businessNumber,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            HorizontalDivider()

            ReceiptInfoRow(
                "Receipt No",
                receipt.receiptNumber
            )

            ReceiptInfoRow(
                "Date",
                receipt.printedAt.formatDateTime()
            )

            ReceiptInfoRow(
                "Cashier",
                receipt.cashierName
            )

            ReceiptInfoRow(
                "Payment",
                receipt.paymentMethod
            )

            receipt.customerName?.let {
                ReceiptInfoRow(
                    "Customer",
                    it
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp)
            )

            receipt.items.forEach { item ->

                Text(
                    item.name,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text(
                        "${item.qty} × ₦${item.price.formatPrice()}"
                    )

                    Text(
                        "₦${item.total.formatPrice()}"
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            HorizontalDivider()

            ReceiptInfoRow(
                "Subtotal",
                "₦${receipt.subtotal.formatPrice()}"
            )

            if (receipt.discount > 0) {
                ReceiptInfoRow(
                    "Discount",
                    "₦${receipt.discount.formatPrice()}"
                )
            }

            val vatAmount =
                (receipt.subtotal *
                        (receipt.taxRate / 100)).toLong()

            if (receipt.taxRate > 0) {

                ReceiptInfoRow(
                    "VAT (${receipt.taxRate}%)",
                    "₦${vatAmount.formatPrice()}"
                )
            }

            HorizontalDivider()

            ReceiptInfoRow(
                "TOTAL",
                "₦${(receipt.subtotal - receipt.discount + vatAmount).formatPrice()}",
                true
            )

            Spacer(Modifier.height(12.dp))

            ReceiptInfoRow(
                "Paid",
                "₦${receipt.amountPaid.formatPrice()}"
            )

            ReceiptInfoRow(
                "Change",
                "₦${receipt.change.formatPrice()}"
            )

            if (receipt.splitPayments.isNotEmpty()) {

                Spacer(Modifier.height(12.dp))

                Text(
                    "Split Payments",
                    fontWeight = FontWeight.Bold
                )

                receipt.splitPayments.forEach {

                    ReceiptInfoRow(
                        it.method,
                        "₦${it.amount.formatPrice()}"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            receipt.footerMessage?.let {

                Text(
                    it,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Text(
                "THANK YOU FOR YOUR PATRONAGE",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ReceiptInfoRow(
    title: String,
    value: String,
    bold: Boolean = false
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text = title,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )

        Text(
            text = value,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}