package com.techsultan.zenithpro.features.category.data.mapper

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.category.data.local.CategoryEntity
import com.techsultan.zenithpro.features.category.data.remote.CategoryDto

fun CategoryDto.toEntity() = CategoryEntity(
    id = id,
    businessId = businessId,
    name = name,
    color = color,
    icon = icon,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = Util.SyncStatus.SYNCED
)

fun CategoryEntity.toDto() = CategoryDto(
    id         = id,
    businessId = businessId,
    name       = name,
    color      = color,
    icon       = icon,
    createdAt  = createdAt,
    updatedAt  = updatedAt,
    deletedAt  = deletedAt
)