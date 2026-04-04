package com.techsultan.zenithpro.features.settings.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_settings")
data class BusinessSettingsEntity(
    @PrimaryKey val businessId: String,
    val currencySymbol: String,
    val currencyCode: String,
    val taxRate: Double,
    val allowNegativeStock: Boolean,
    val requireCustomerSale: Boolean,
    val lowStockThreshold: Int,
    val receiptFooter: String?,
    val updatedAt: String
)