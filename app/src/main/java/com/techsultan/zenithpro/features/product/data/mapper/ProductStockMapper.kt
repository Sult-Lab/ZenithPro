package com.techsultan.zenithpro.features.product.data.mapper

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.product.data.local.ProductEntity
import com.techsultan.zenithpro.features.product.data.local.ProductStockEntity
import com.techsultan.zenithpro.features.product.data.local.ProductVariantEntity
import com.techsultan.zenithpro.features.product.data.remote.ProductDto
import com.techsultan.zenithpro.features.product.data.remote.ProductStockDto
import com.techsultan.zenithpro.features.product.data.remote.ProductVariantDto

fun ProductStockDto.toEntity() = ProductStockEntity(
    id = id,
    variantId = variantId,
    quantity = quantity,
    expiryDate = expiryDate,
    lowStockAlert = lowStockAlert,
    updatedAt = updatedAt,
    syncStatus = Util.SyncStatus.SYNCED
)