package com.techsultan.zenithpro.features.expenses.data.local

import java.time.LocalDate

data class ExpenseFilter(
    val from: LocalDate = LocalDate.now().withDayOfMonth(1),
    val to: LocalDate = LocalDate.now(),
    val category: String? = null,
    val minAmount: Long? = null,
    val maxAmount: Long? = null
)