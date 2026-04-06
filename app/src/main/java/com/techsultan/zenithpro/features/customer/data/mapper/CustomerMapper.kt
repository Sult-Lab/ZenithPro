package com.techsultan.zenithpro.features.customer.data.mapper

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.data.remote.CustomerDto

fun CustomerDto.toEntity() = CustomerEntity(
    id = id,
    businessId = businessId,
    firstName = firstName,
    lastName = lastName,
    phone = phone,
    email = email,
    address = address,
    totalSpent = totalSpent,
    totalDebt = totalDebt,
    visitCount = visitCount,
    lastVisitAt = lastVisitAt,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = Util.SyncStatus.SYNCED
)

fun CustomerEntity.toDto() = CustomerDto(
    id          = id,
    businessId  = businessId,
    firstName   = firstName,
    lastName    = lastName,
    phone       = phone,
    email       = email,
    address     = address,
    totalSpent  = totalSpent,
    totalDebt   = totalDebt,
    visitCount  = visitCount,
    lastVisitAt = lastVisitAt,
    notes       = notes,
    createdAt   = createdAt,
    updatedAt   = updatedAt,
    deletedAt   = deletedAt
)