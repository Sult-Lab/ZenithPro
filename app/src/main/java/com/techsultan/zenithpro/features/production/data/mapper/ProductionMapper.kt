package com.techsultan.zenithpro.features.production.data.mapper

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.production.data.local.ProductionOrderEntity
import com.techsultan.zenithpro.features.production.data.remote.ProductionOrderDto

fun ProductionOrderDto.toEntity() = ProductionOrderEntity(
    id = id, businessId = businessId, branchId = branchId,
    variantId = variantId, quantity = quantity, status = status,
    notes = notes, startedAt = startedAt, completedAt = completedAt,
    createdBy = createdBy, createdAt = createdAt, updatedAt = updatedAt,
    syncStatus = Util.SyncStatus.SYNCED
)