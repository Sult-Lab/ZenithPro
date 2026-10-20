package com.techsultan.zenithpro.core.database

import androidx.room.TypeConverter
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.sales.FulfillmentStatus
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.SaleStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromList(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toList(value: String): List<String> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromSyncStatus(value: String): Util.SyncStatus =
        runCatching { Util.SyncStatus.valueOf(value) }.getOrDefault(Util.SyncStatus.SYNCED)

    @TypeConverter
    fun toSyncStatus(status: Util.SyncStatus): String = status.name

    @TypeConverter
    fun fromSaleStatus(status: SaleStatus): String = status.name

    @TypeConverter
    fun toSaleStatus(value: String?): SaleStatus =
        value?.let { v -> SaleStatus.entries.find { it.name.equals(v, ignoreCase = true) } } ?: SaleStatus.COMPLETED

    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod): String = method.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod =
        value?.let { v -> PaymentMethod.entries.find { it.name.equals(v, ignoreCase = true) } } ?: PaymentMethod.CASH

    @TypeConverter
    fun fromFulfillmentStatus(status: FulfillmentStatus): String = status.name

    @TypeConverter
    fun toFulfillmentStatus(value: String?): FulfillmentStatus = FulfillmentStatus.fromString(value)
}
