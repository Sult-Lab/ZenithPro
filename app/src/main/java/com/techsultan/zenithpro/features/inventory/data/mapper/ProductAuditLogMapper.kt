package com.techsultan.zenithpro.features.inventory.data.mapper

import com.techsultan.zenithpro.features.inventory.data.local.ProductAuditLogEntity
import com.techsultan.zenithpro.features.inventory.data.remote.ProductAuditLogDto

fun ProductAuditLogDto.toEntity() = ProductAuditLogEntity(
    id = id,
    productId = productId,
    businessId = businessId,
    changedBy = changedBy,
    changedByName = changedByName,
    changedByRole = changedByRole,
    changeType = changeType,
    fieldChanged = fieldChanged,
    oldValue = oldValue,
    newValue = newValue,
    variantSku = variantSku,
    notes = notes,
    createdAt = createdAt
)
