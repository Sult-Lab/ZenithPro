package com.techsultan.zenithpro.features.sales.domain.use_case

import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.data.local.ReceiptItem
import com.techsultan.zenithpro.features.sales.data.remote.CompletedSale

class GenerateReceiptUseCase {

    operator fun invoke(
        sale: CompletedSale
    ): ReceiptData {

        return ReceiptData(
            receiptNumber = sale.receiptNumber,
            cashierName = sale.salesPerson,
            customerName = sale.customer?.fullName,
            paymentMethod = sale.paymentMethod,
            items = sale.cartItems.map {

                ReceiptItem(
                    name = it.productName,
                    qty = it.quantity,
                    price = it.unitPrice,
                    total = it.unitPrice * it.quantity
                )
            },
            subtotal = sale.subtotal,
            discount = sale.discount,
            total = sale.total,
            amountPaid = sale.amountPaid,
            change = sale.change,
            splitPayments = sale.splitPayments,
            createdAt = sale.createdAt,
            businessName = sale.businessName,
            businessAddress = sale.businessAddress,
            businessNumber = sale.businessNumber
        )
    }
}