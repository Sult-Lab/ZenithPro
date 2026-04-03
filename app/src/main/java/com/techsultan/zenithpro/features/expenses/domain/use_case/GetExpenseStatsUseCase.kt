package com.techsultan.zenithpro.features.expenses.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseFilter
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseStatsData
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseSummary
import com.techsultan.zenithpro.features.expenses.domain.repository.ExpenseRepository

class GetExpenseStatsUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(
        businessId: String,
        filter: ExpenseFilter
    ): Resource<ExpenseStatsData> = try {
        val summaryResult = repository.getSummary(businessId, filter)
        val breakdownResult = repository.getCategoryBreakdown(businessId, filter)
        val categories = repository.getCategories(businessId)

        Resource.Success(
            ExpenseStatsData(
                summary = (summaryResult as? Resource.Success)?.data ?: ExpenseSummary(),
                breakdown = (breakdownResult as? Resource.Success)?.data ?: emptyList(),
                categories = categories
            )
        )
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed")
    }
}