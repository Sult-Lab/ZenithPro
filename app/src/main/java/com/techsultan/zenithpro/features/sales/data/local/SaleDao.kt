package com.techsultan.zenithpro.features.sales.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.techsultan.zenithpro.features.dashboard.data.remote.ChartDataPoint
import com.techsultan.zenithpro.features.dashboard.data.remote.DashboardSummary
import com.techsultan.zenithpro.features.dashboard.data.remote.PendingDebtSummary
import com.techsultan.zenithpro.features.sales.data.remote.DailySummary
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<SaleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Transaction
    @Query("""
        SELECT * FROM sales 
        WHERE businessId = :businessId 
        ORDER BY soldAt DESC
    """)
    fun getSales(businessId: String): Flow<List<SaleWithItems>>

    @Transaction
    @Query("""
        SELECT * FROM sales
        WHERE businessId = :businessId
          AND soldAt BETWEEN :from AND :to
        ORDER BY soldAt DESC
    """)
    fun getSalesByDateRange(
        businessId: String,
        from: String,
        to: String
    ): Flow<List<SaleWithItems>>

    @Transaction
    @Query("""
    SELECT * FROM sales
    WHERE businessId  = :businessId
      AND soldAt      BETWEEN :from AND :to
      AND (:staffId        IS NULL OR staffId       = :staffId)
      AND (:paymentMethod  IS NULL OR paymentMethod = :paymentMethod)
      AND (:branchId       IS NULL OR branchId      = :branchId)
    ORDER BY soldAt DESC
""")
    fun getSalesFiltered(
        businessId: String,
        from: String,
        to: String,
        staffId: String?,
        paymentMethod: String?,
        branchId: String?
    ): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getSaleById(saleId: String): SaleWithItems?

    @Query("SELECT * FROM sales WHERE syncStatus = 'PENDING'")
    suspend fun getUnsyncedSales(): List<SaleEntity>

    @Query("UPDATE sales SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT id FROM sales WHERE businessId = :businessId")
    suspend fun getAllSaleIds(businessId: String): List<String>

    // Today's summary for dashboard
    @Query("""
        SELECT 
            COALESCE(SUM(totalAmount), 0)   AS totalRevenue,
            COALESCE(SUM(amountPaid), 0)    AS totalCollected,
            COALESCE(SUM(debtAmount), 0)    AS totalDebt,
            COUNT(*)                         AS orderCount
        FROM sales
        WHERE businessId = :businessId
          AND soldAt >= :startOfDay
          AND status != 'CANCELLED'
    """)
    suspend fun getDailySummary(businessId: String, startOfDay: String): DailySummary

    // Today's summary — computed entirely from local Room
    @Query("""
    SELECT
        COALESCE(SUM(s.totalAmount), 0)                         AS totalRevenue,
        COALESCE(SUM(s.amountPaid), 0)                          AS totalCollected,
        COALESCE(SUM(s.debtAmount), 0)                          AS totalDebt,
        COUNT(s.id)                                              AS totalOrders,
        COALESCE(SUM(si.totalPrice - (si.costPrice * si.quantity)), 0) AS totalProfit,
        COALESCE(SUM(si.costPrice * si.quantity), 0)             AS totalCost
    FROM sales s
    INNER JOIN sale_items si ON si.saleId = s.id
    WHERE s.businessId  = :businessId
      AND s.soldAt      >= :startOfDay
      AND s.status      != 'CANCELLED'
""")
    suspend fun getTodaySummary(businessId: String, startOfDay: String): DashboardSummary

    // Last N days — one row per day for the chart
    @Query("""
    SELECT
        DATE(s.soldAt)                                               AS saleDate,
        COALESCE(SUM(s.totalAmount), 0)                              AS revenue,
        COALESCE(SUM(si.totalPrice - (si.costPrice * si.quantity)), 0) AS profit,
        COUNT(DISTINCT s.id)                                         AS orderCount
    FROM sales s
    INNER JOIN sale_items si ON si.saleId = s.id
    WHERE s.businessId  = :businessId
      AND s.soldAt      >= :since
      AND s.status      != 'CANCELLED'
    GROUP BY DATE(s.soldAt)
    ORDER BY saleDate ASC
""")
    suspend fun getChartData(businessId: String, since: String): List<ChartDataPoint>

    // Pending debts count and total
    @Query("""
    SELECT
        COUNT(*)        AS debtCount,
        SUM(debtAmount) AS totalDebt
    FROM sales
    WHERE businessId = :businessId
      AND status     = 'PARTIAL'
""")
    suspend fun getPendingDebts(businessId: String): PendingDebtSummary

    @Transaction
    @Query("""
    SELECT * FROM sales
    WHERE customerId = :customerId
      AND status     != 'CANCELLED'
    ORDER BY soldAt DESC
""")
    suspend fun getSalesByCustomer(customerId: String): List<SaleWithItems>
}