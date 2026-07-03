package com.techsultan.zenithpro.core.util


object PaymentReferenceGenerator {

    fun generate(counter: Int): String {
        return "ZP-${counter.toString().padStart(5, '0')}"
    }
}