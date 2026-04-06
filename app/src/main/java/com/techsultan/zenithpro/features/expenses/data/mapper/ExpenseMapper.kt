package com.techsultan.zenithpro.features.expenses.data.mapper

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseEntity
import com.techsultan.zenithpro.features.expenses.data.remote.ExpenseDto

fun ExpenseDto.toEntity() = ExpenseEntity(
    id = id,
    businessId = businessId,
    branchId = branchId,
    recordedBy = recordedBy,
    title = title,
    amount = amount,
    category = category,
    notes = notes,
    receiptUrl = receiptUrl,
    expenseDate = expenseDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = Util.SyncStatus.SYNCED
)

fun ExpenseEntity.toDto() = ExpenseDto(
    id          = id,
    businessId  = businessId,
    branchId    = branchId,
    recordedBy  = recordedBy,
    title       = title,
    amount      = amount,
    category    = category,
    notes       = notes,
    receiptUrl  = receiptUrl,
    expenseDate = expenseDate,
    createdAt   = createdAt,
    updatedAt   = updatedAt,
    deletedAt   = deletedAt
)