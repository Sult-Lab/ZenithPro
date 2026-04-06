package com.techsultan.zenithpro.features.expenses.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseEntity
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseFilter
import com.techsultan.zenithpro.features.expenses.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

class GetExpensesUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(
        businessId: String,
        filter: ExpenseFilter
    ): Flow<Resource<List<ExpenseEntity>>> =
        repository.getExpenses(businessId, filter)
}