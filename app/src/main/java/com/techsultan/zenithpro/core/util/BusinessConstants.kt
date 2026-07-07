package com.techsultan.zenithpro.core.util

object BusinessConstants {

    val BUSINESS_TYPES = listOf(
        "Retail Store",
        "Pharmacy",
        "Supermarket / Grocery",
        "Restaurant / Food",
        "Bakery / Confectionery",
        "Fashion / Clothing",
        "Electronics",
        "Hardware / Building Materials",
        "Cosmetics / Beauty",
        "Agriculture / Farm",
        "Wholesale / Distribution",
        "Printing / Stationery",
        "Auto Parts / Mechanics",
        "Furniture / Interior",
        "Bookshop",
        "Hospital / Clinic",
        "Hotel / Hospitality",
        "Salon / Barbershop",
        "Logistics / Delivery",
        "Manufacturing",
        "Tech / Software",
        "Education / School",
        "Other"
    )

    data class CurrencyOption(
        val code: String,
        val symbol: String,
        val name: String
    ) {
        override fun toString() = "$name ($code)"
    }

    val CURRENCIES = listOf(
        CurrencyOption("NGN", "₦",  "Nigerian Naira"),
        CurrencyOption("USD", "$",  "US Dollar"),
        CurrencyOption("GBP", "£",  "British Pound"),
        CurrencyOption("EUR", "€",  "Euro"),
        CurrencyOption("GHS", "₵",  "Ghanaian Cedi"),
        CurrencyOption("KES", "KSh","Kenyan Shilling"),
        CurrencyOption("ZAR", "R",  "South African Rand"),
        CurrencyOption("TZS", "TSh","Tanzanian Shilling"),
        CurrencyOption("UGX", "USh","Ugandan Shilling"),
        CurrencyOption("XOF", "CFA","West African CFA"),
        CurrencyOption("RWF", "RF", "Rwandan Franc"),
        CurrencyOption("ETB", "Br", "Ethiopian Birr")
    )
}