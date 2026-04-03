package com.techsultan.zenithpro.features.expenses.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseEntity
import com.techsultan.zenithpro.features.expenses.data.remote.UpsertExpenseRequest
import com.techsultan.zenithpro.features.expenses.domain.repository.ExpenseRepository

class UpsertExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(
        request: UpsertExpenseRequest,
        businessId: String,
        staffId: String
    ): Resource<ExpenseEntity> {
        if (request.title.isBlank()) return Resource.Error("Title is required")
        if (request.amount <= 0) return Resource.Error("Amount must be greater than zero")
        if (request.category.isBlank()) return Resource.Error("Category is required")
        return repository.upsertExpense(request, businessId, staffId)
    }
}