package com.techsultan.zenithpro.features.expenses.domain.repository

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.expenses.data.local.CategoryBreakdown
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseEntity
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseFilter
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseSummary
import com.techsultan.zenithpro.features.expenses.data.remote.UpsertExpenseRequest
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getExpenses(
        businessId: String,
        filter: ExpenseFilter
    ): Flow<Resource<List<ExpenseEntity>>>

    suspend fun upsertExpense(
        request: UpsertExpenseRequest,
        businessId: String,
        staffId: String
    ): Resource<ExpenseEntity>

    suspend fun deleteExpense(expenseId: String): Resource<Unit>
    suspend fun getCategories(businessId: String): List<String>
    suspend fun getSummary(businessId: String, filter: ExpenseFilter): Resource<ExpenseSummary>
    suspend fun getCategoryBreakdown(
        businessId: String,
        filter: ExpenseFilter
    ): Resource<List<CategoryBreakdown>>
    suspend fun pullFromServer(businessId: String): Resource<Unit>
}