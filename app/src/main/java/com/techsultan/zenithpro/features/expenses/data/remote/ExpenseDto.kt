package com.techsultan.zenithpro.features.expenses.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseDto(
    val id: String,
    @SerialName("business_id")  val businessId: String,
    @SerialName("branch_id")    val branchId: String?,
    @SerialName("recorded_by")  val recordedBy: String,
    val title: String,
    val amount: Long,
    val category: String,
    val notes: String?,
    @SerialName("receipt_url")  val receiptUrl: String?,
    @SerialName("expense_date") val expenseDate: String,
    @SerialName("created_at")   val createdAt: String,
    @SerialName("updated_at")   val updatedAt: String,
    @SerialName("deleted_at")   val deletedAt: String? = null
)