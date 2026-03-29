package com.techsultan.zenithpro.core.database

import androidx.room.TypeConverter
import com.techsultan.zenithpro.core.util.Util
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
    fun fromSyncStatus(value: String): Util.SyncStatus = Util.SyncStatus.valueOf(value)

    @TypeConverter
    fun toSyncStatus(status: Util.SyncStatus): String = status.name
}