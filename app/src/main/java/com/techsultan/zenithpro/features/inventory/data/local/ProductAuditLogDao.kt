package com.techsultan.zenithpro.features.inventory.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductAuditLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<ProductAuditLogEntity>)

    @Query("""
        SELECT * FROM product_audit_log
        WHERE productId = :productId
        ORDER BY createdAt DESC
        LIMIT 50
    """)
    fun observeLogsForProduct(productId: String): Flow<List<ProductAuditLogEntity>>

    @Query("DELETE FROM product_audit_log WHERE productId = :productId")
    suspend fun deleteForProduct(productId: String)
}