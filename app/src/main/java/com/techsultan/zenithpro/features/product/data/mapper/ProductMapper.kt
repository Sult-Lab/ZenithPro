package com.techsultan.zenithpro.features.product.data.mapper

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.product.data.local.ProductEntity
import com.techsultan.zenithpro.features.product.data.remote.ProductDto

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