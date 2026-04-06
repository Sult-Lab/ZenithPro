package com.techsultan.zenithpro.features.material.data.mapper

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.material.data.local.MaterialEntity
import com.techsultan.zenithpro.features.material.data.remote.MaterialDto


fun MaterialDto.toEntity() = MaterialEntity(
    id = id, businessId = businessId, name = name,
    description = description, unit = unit, quantity = quantity,
    costPerUnit = costPerUnit, lowStockAlert = lowStockAlert,
    status = status, supplier = supplier, notes = notes,
    createdAt = createdAt, updatedAt = updatedAt,
    deletedAt = deletedAt, syncStatus = Util.SyncStatus.SYNCED
)

fun MaterialEntity.toDto() = MaterialDto(
    id = id, businessId = businessId, name = name,
    description = description, unit = unit, quantity = quantity,
    costPerUnit = costPerUnit, lowStockAlert = lowStockAlert,
    status = status, supplier = supplier, notes = notes,
    createdAt = createdAt, updatedAt = updatedAt, deletedAt = deletedAt
)