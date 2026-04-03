package com.techsultan.zenithpro.features.expenses.data.local

data class ExpenseStatsData(
    val summary: ExpenseSummary,
    val breakdown: List<CategoryBreakdown>,
    val categories: List<String>
)