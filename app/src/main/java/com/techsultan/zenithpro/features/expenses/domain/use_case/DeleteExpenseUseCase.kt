package com.techsultan.zenithpro.features.expenses.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.expenses.domain.repository.ExpenseRepository

class DeleteExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(expenseId: String): Resource<Unit> =
        repository.deleteExpense(expenseId)
}