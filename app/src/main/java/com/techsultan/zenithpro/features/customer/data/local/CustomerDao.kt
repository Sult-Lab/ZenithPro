package com.techsultan.zenithpro.features.customer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.techsultan.zenithpro.features.customer.data.remote.CustomerStats
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    // ── Observe ────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM customers
        WHERE businessId  = :businessId
          AND deletedAt   IS NULL
          AND syncStatus  != 'DELETED'
        ORDER BY firstName ASC
    """)
    fun getAllCustomers(businessId: String): Flow<List<CustomerEntity>>

    @Query("""
        SELECT * FROM customers
        WHERE businessId  = :businessId
          AND deletedAt   IS NULL
          AND syncStatus  != 'DELETED'
          AND (
              firstName  LIKE '%' || :query || '%'
              OR lastName  LIKE '%' || :query || '%'
              OR phone     LIKE '%' || :query || '%'
          )
        ORDER BY firstName ASC
    """)
    fun searchCustomers(businessId: String, query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id")
    fun observeCustomer(id: String): Flow<CustomerEntity?>

    // ── Sync helpers ───────────────────────────────────────────────

    @Query("SELECT * FROM customers WHERE syncStatus IN ('PENDING','DIRTY','DELETED')")
    suspend fun getUnsyncedCustomers(): List<CustomerEntity>

    @Query("UPDATE customers SET syncStatus = 'SYNCED', updatedAt = :at WHERE id = :id")
    suspend fun markSynced(id: String, at: String)

    @Query("UPDATE customers SET deletedAt = :at, syncStatus = 'DELETED' WHERE id = :id")
    suspend fun softDelete(id: String, at: String)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT id FROM customers WHERE businessId = :businessId")
    suspend fun getAllCustomerIds(businessId: String): List<String>

    // ── Reports ────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM customers
        WHERE businessId = :businessId
          AND deletedAt  IS NULL
        ORDER BY totalSpent DESC
        LIMIT :limit
    """)
    suspend fun getTopSpenders(businessId: String, limit: Int = 10): List<CustomerEntity>

    @Query("""
        SELECT * FROM customers
        WHERE businessId = :businessId
          AND deletedAt  IS NULL
        ORDER BY visitCount DESC
        LIMIT :limit
    """)
    suspend fun getMostLoyal(businessId: String, limit: Int = 10): List<CustomerEntity>

    @Query("""
        SELECT * FROM customers
        WHERE businessId = :businessId
          AND totalDebt  > 0
          AND deletedAt  IS NULL
        ORDER BY totalDebt DESC
    """)
    fun getCustomersWithDebt(businessId: String): Flow<List<CustomerEntity>>

    @Query("""
        SELECT
            COUNT(*)       AS totalCustomers,
            SUM(totalSpent) AS totalRevenue,
            SUM(totalDebt)  AS totalDebt,
            SUM(visitCount) AS totalVisits
        FROM customers
        WHERE businessId = :businessId
          AND deletedAt  IS NULL
    """)
    suspend fun getCustomerStats(businessId: String): CustomerStats
}