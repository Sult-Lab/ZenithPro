package com.techsultan.zenithpro.features.settings.data.mapper

import com.techsultan.zenithpro.features.settings.data.local.BusinessSettingsEntity
import com.techsultan.zenithpro.features.settings.data.remote.BusinessSettingsDto

fun BusinessSettingsDto.toEntity() = BusinessSettingsEntity(
    businessId = businessId,
    currencySymbol = currencySymbol,
    currencyCode = currencyCode,
    taxRate = taxRate,
    allowNegativeStock = allowNegativeStock,
    requireCustomerSale = requireCustomerSale,
    lowStockThreshold = lowStockThreshold,
    receiptFooter = receiptFooter,
    updatedAt = updatedAt
)

fun BusinessSettingsEntity.toDto() = BusinessSettingsDto(
    businessId = businessId,
    currencySymbol = currencySymbol,
    currencyCode = currencyCode,
    taxRate = taxRate,
    allowNegativeStock = allowNegativeStock,
    requireCustomerSale = requireCustomerSale,
    lowStockThreshold = lowStockThreshold,
    receiptFooter = receiptFooter,
    updatedAt = updatedAt
)
