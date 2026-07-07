package com.techsultan.zenithpro.features.inventory.data.mapper

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.inventory.data.local.ProductStockEntity
import com.techsultan.zenithpro.features.inventory.data.remote.ProductStockDto

fun ProductStockDto.toEntity() = ProductStockEntity(
    id = id,
    variantId = variantId,
    quantity = quantity,
    expiryDate = expiryDate,
    lowStockAlert = lowStockAlert,
    updatedAt = updatedAt,
    syncStatus = Util.SyncStatus.SYNCED
)