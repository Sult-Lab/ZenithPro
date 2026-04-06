package com.techsultan.zenithpro.features.product.data.mapper

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.product.data.local.ProductEntity
import com.techsultan.zenithpro.features.product.data.local.ProductVariantEntity
import com.techsultan.zenithpro.features.product.data.remote.ProductDto
import com.techsultan.zenithpro.features.product.data.remote.ProductVariantDto

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