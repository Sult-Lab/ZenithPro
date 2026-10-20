package com.techsultan.zenithpro.features.production.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductionOrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: ProductionOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<ProductionOrderEntity>)

    @Query("""
        SELECT * FROM production_orders
        WHERE businessId = :businessId
          AND (:status IS NULL OR status = :status)
        ORDER BY createdAt DESC
    """)
    fun getOrders(businessId: String, status: String?): Flow<List<ProductionOrderEntity>>

    @Query("SELECT * FROM production_orders WHERE id = :id")
    suspend fun getOrderById(id: String): ProductionOrderEntity?

    @Query("""
        SELECT COUNT(*) FROM production_orders
        WHERE businessId = :businessId
          AND status IN ('DRAFT','IN_PROGRESS')
    """)
    suspend fun getPendingOrderCount(businessId: String): Int

    @Query("SELECT * FROM production_orders WHERE syncStatus IN ('PENDING','DIRTY')")
    suspend fun getUnsyncedOrders(): List<ProductionOrderEntity>

    @Query("UPDATE production_orders SET syncStatus = 'SYNCED', updatedAt = :at WHERE id = :id")
    suspend fun markSynced(id: String, at: String)

    @Query("UPDATE production_orders SET status = :status, updatedAt = :at WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, at: String)

    @Query("UPDATE production_orders SET status = :status, startedAt = :startedAt, materialsDeducted = 1, updatedAt = :at WHERE id = :id")
    suspend fun updateStatusAndStartedAt(id: String, status: String, startedAt: String, at: String)

    @Query("UPDATE production_orders SET status = :status, cancelledAt = :cancelledAt, materialsDeducted = 0, updatedAt = :at WHERE id = :id")
    suspend fun updateStatusAndCancelledAt(id: String, status: String, cancelledAt: String, at: String)

    @Query("""
        UPDATE production_orders
        SET status = 'COMPLETED',
            actualQuantity = :actualQuantity,
            remarks = :remarks,
            completedAt = :completedAt,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateCompletedOrder(
        id: String,
        actualQuantity: Double,
        remarks: String?,
        completedAt: String,
        updatedAt: String
    )
}
