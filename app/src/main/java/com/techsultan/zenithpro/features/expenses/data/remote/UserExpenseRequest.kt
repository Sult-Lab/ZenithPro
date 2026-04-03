package com.techsultan.zenithpro.features.expenses.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class UpsertExpenseRequest(
    val id: String,
    val title: String,
    val amount: Long,
    val category: String,
    val notes: String?,
    val expenseDate: String,
    val branchId: String?
)