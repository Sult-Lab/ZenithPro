package com.techsultan.zenithpro.features.inventory.data.mapper

import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.inventory.data.local.ProductEntity
import com.techsultan.zenithpro.features.inventory.data.remote.ProductDto

fun ProductDto.toProductEntity() = ProductEntity(
    id = id,
    businessId = businessId,
    name = name,
    description = description,
    category = category,
    baseSalesPrice = baseSalesPrice,
    baseCostPrice = baseCostPrice,
    imageUrl = imageUrl,
    isActive = isActive,
    imageUrls = imageUrls,
    expiryWarningDays = expiryWarningDays,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = Util.SyncStatus.SYNCED
)

fun ProductEntity.toProductDto(): ProductDto {
    return ProductDto(
        id = id,
        businessId = businessId,
        name = name,
        description = description,
        category = category,
        baseSalesPrice = baseSalesPrice,
        baseCostPrice = baseCostPrice,
        imageUrl = imageUrl,
        isActive = isActive,
        imageUrls = imageUrls,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        expiryWarningDays = expiryWarningDays
    )
}