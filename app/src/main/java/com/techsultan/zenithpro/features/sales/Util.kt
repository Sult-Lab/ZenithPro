package com.techsultan.zenithpro.features.sales

import java.text.NumberFormat
import java.util.Locale

enum class PaymentMethod {
    CASH,
    TRANSFER,
    POS,
    USSD,
    SPLIT,
    DEBT,
    CARD
}
enum class SaleStatus { COMPLETED, PARTIAL, REFUNDED, CANCELLED }

enum class PaymentStep {
    CART,           // viewing cart
    SELECT_CUSTOMER, // searching/selecting customer
    PAYMENT,        // entering payment details
    CONFIRM         // review before submitting
}

fun Long.formatAmount(): String = NumberFormat
    .getNumberInstance(Locale("en", "NG"))
    .apply { maximumFractionDigits = 0 }
    .format(this)