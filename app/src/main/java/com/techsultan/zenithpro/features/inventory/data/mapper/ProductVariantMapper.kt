package com.techsultan.zenithpro.features.inventory.data.mapper

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.inventory.data.local.ProductVariantEntity
import com.techsultan.zenithpro.features.inventory.data.remote.ProductVariantDto

fun ProductVariantDto.toEntity() = ProductVariantEntity(
    id = id,
    productId = productId,
    businessId = businessId,
    sku = sku,
    salesPrice = salesPrice,
    costPrice = costPrice,
    barcode = barcode,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = Util.SyncStatus.SYNCED
)