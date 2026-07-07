package com.techsultan.zenithpro.core.manager

import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ReceiptNumberGenerator(
    private val dataStore: AppDataStore
) {

    suspend fun generate(): String {
        val today = LocalDate.now()
            .format(DateTimeFormatter.BASIC_ISO_DATE)

        val current = dataStore.receiptCounter.first()
        val next = current + 1

        dataStore.saveReceiptCounter(next)

        return "RCP-$today-${next.toString().padStart(4, '0')}"
    }
}
