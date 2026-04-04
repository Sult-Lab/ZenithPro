package com.techsultan.zenithpro.features.branch.data.mapper

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.branch.data.remote.BranchDto

fun BranchDto.toEntity() = BranchEntity(
    id = id, businessId = businessId, name = name,
    address = address, phone = phone, isActive = isActive,
    createdAt = createdAt, updatedAt = updatedAt,
    deletedAt = deletedAt, syncStatus = Util.SyncStatus.SYNCED
)

fun BranchEntity.toDto() = BranchDto(
    id = id, businessId = businessId, name = name,
    address = address, phone = phone, isActive = isActive,
    createdAt = createdAt, updatedAt = updatedAt, deletedAt = deletedAt
)