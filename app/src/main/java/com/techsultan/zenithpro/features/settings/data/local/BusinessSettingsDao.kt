package com.techsultan.zenithpro.features.settings.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: BusinessSettingsEntity)

    @Query("SELECT * FROM business_settings WHERE businessId = :businessId")
    fun observeSettings(businessId: String): Flow<BusinessSettingsEntity?>

    @Query("SELECT * FROM business_settings WHERE businessId = :businessId")
    suspend fun getSettings(businessId: String): BusinessSettingsEntity?
}