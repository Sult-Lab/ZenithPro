package com.techsultan.zenithpro.core.util

object NigerianBanks {
    data class Bank(val name: String, val code: String)

    val ALL = listOf(
        Bank("Access Bank",            "044"),
        Bank("Citibank",               "023"),
        Bank("Ecobank",                "050"),
        Bank("Fidelity Bank",          "070"),
        Bank("First Bank",             "011"),
        Bank("First City Monument Bank","214"),
        Bank("Globus Bank",            "103"),
        Bank("Guaranty Trust Bank",    "058"),
        Bank("Heritage Bank",          "030"),
        Bank("Keystone Bank",          "082"),
        Bank("Kuda Bank",              "090267"),
        Bank("Moniepoint",             "090405"),
        Bank("OPay",                   "100004"),
        Bank("Opay Digital Services",  "304"),
        Bank("Palmpay",                "100033"),
        Bank("Polaris Bank",           "076"),
        Bank("Providus Bank",          "101"),
        Bank("Stanbic IBTC Bank",      "221"),
        Bank("Standard Chartered",     "068"),
        Bank("Sterling Bank",          "232"),
        Bank("SunTrust Bank",          "100"),
        Bank("Union Bank",             "032"),
        Bank("United Bank for Africa", "033"),
        Bank("Unity Bank",             "215"),
        Bank("VFD Microfinance Bank",  "566"),
        Bank("Wema Bank",              "035"),
        Bank("Zenith Bank",            "057"),
    )
}