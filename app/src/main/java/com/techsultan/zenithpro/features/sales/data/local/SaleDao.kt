package com.techsultan.zenithpro.features.sales.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.techsultan.zenithpro.features.analytics.data.ReportSalesSummary
import com.techsultan.zenithpro.features.analytics.data.StaffSalesRow
import com.techsultan.zenithpro.features.analytics.data.TopProductRow
import com.techsultan.zenithpro.features.dashboard.data.remote.ChartDataPoint
import com.techsultan.zenithpro.features.dashboard.data.remote.DashboardSummary
import com.techsultan.zenithpro.features.dashboard.data.remote.PendingDebtSummary
import com.techsultan.zenithpro.features.sales.data.remote.DailySummary
import com.techsultan.zenithpro.features.sales.SaleStatus
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

    @Transaction
    @Query("SELECT * FROM sales WHERE syncStatus = 'PENDING'")
    suspend fun getUnsyncedSalesWithItems(): List<SaleWithItems>

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
      AND (:branchId    IS NULL OR s.branchId = :branchId)
""")
    suspend fun getTodaySummary(
        businessId: String,
        startOfDay: String,
        branchId: String? = null
    ): DashboardSummary

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
      AND (:branchId    IS NULL OR s.branchId = :branchId)
    GROUP BY DATE(s.soldAt)
    ORDER BY saleDate ASC
""")
    suspend fun getChartData(
        businessId: String,
        since: String,
        branchId: String? = null
    ): List<ChartDataPoint>

    // Pending debts count and total
    @Query("""
    SELECT
        COUNT(*)        AS debtCount,
        SUM(debtAmount) AS totalDebt
    FROM sales
    WHERE businessId = :businessId
      AND status     = 'PARTIAL'
      AND (:branchId   IS NULL OR branchId = :branchId)
""")
    suspend fun getPendingDebts(
        businessId: String,
        branchId: String? = null
    ): PendingDebtSummary

    @Transaction
    @Query("""
    SELECT * FROM sales
    WHERE customerId = :customerId
      AND status     != 'CANCELLED'
    ORDER BY soldAt DESC
""")
    suspend fun getSalesByCustomer(customerId: String): List<SaleWithItems>

    // SaleDao.kt — add filtered summary query
    @Query("""
    SELECT
        COALESCE(SUM(s.totalAmount), 0)                              AS totalRevenue,
        COALESCE(SUM(s.amountPaid), 0)                               AS totalCollected,
        COALESCE(SUM(s.debtAmount), 0)                               AS totalDebt,
        COUNT(s.id)                                                   AS totalOrders,
        COALESCE(SUM(si_summary.profit), 0)                          AS totalProfit,
        COALESCE(SUM(si_summary.cost), 0)                            AS totalCost,
        CASE 
            WHEN SUM(s.totalAmount) > 0 
            THEN (SUM(si_summary.profit) * 100.0) / SUM(s.totalAmount) 
            ELSE 0 
        END AS profitMargin
    FROM sales s
    LEFT JOIN (
        SELECT saleId, 
               SUM(totalPrice - (costPrice * quantity)) AS profit,
               SUM(costPrice * quantity) AS cost
        FROM sale_items
        GROUP BY saleId
    ) si_summary ON s.id = si_summary.saleId
    WHERE s.businessId   = :businessId
      AND s.soldAt       BETWEEN :from AND :to
      AND s.status      != 'CANCELLED'
      AND (:branchId     IS NULL OR s.branchId = :branchId)
      AND (:staffId      IS NULL OR s.staffId  = :staffId)
""")
    suspend fun getSummaryFiltered(
        businessId: String,
        from: String,
        to: String,
        branchId: String?,
        staffId: String?
    ): ReportSalesSummary

    @Query("""
    SELECT
        DATE(s.soldAt)  AS saleDate,
        COALESCE(SUM(s.totalAmount), 0) AS revenue,
        COALESCE(SUM(si_summary.profit), 0) AS profit,
        COUNT(s.id)  AS orderCount
    FROM sales s
    LEFT JOIN (
        SELECT saleId, 
               SUM(totalPrice - (costPrice * quantity)) AS profit
        FROM sale_items
        GROUP BY saleId
    ) si_summary ON s.id = si_summary.saleId
    WHERE s.businessId   = :businessId
      AND s.soldAt       BETWEEN :from AND :to
      AND s.status      != 'CANCELLED'
      AND (:branchId     IS NULL OR s.branchId = :branchId)
      AND (:staffId      IS NULL OR s.staffId  = :staffId)
    GROUP BY DATE(s.soldAt)
    ORDER BY saleDate ASC
""")
    suspend fun getChartDataFiltered(
        businessId: String,
        from: String,
        to: String,
        branchId: String?,
        staffId: String?
    ): List<ChartDataPoint>

    // Top products in date range — used for "best sellers" panel
    @Query("""
    SELECT
        si.productName                  AS productName,
        SUM(si.quantity)                AS unitsSold,
        SUM(si.totalPrice)              AS revenue,
        SUM(si.totalPrice - (si.costPrice * si.quantity)) AS profit
    FROM sale_items si
    INNER JOIN sales s ON s.id = si.saleId
    WHERE s.businessId   = :businessId
      AND s.soldAt       BETWEEN :from AND :to
      AND s.status      != 'CANCELLED'
      AND (:branchId     IS NULL OR s.branchId = :branchId)
      AND (:staffId      IS NULL OR s.staffId  = :staffId)
    GROUP BY si.productName
    ORDER BY revenue DESC
    LIMIT :limit
""")
    suspend fun getTopProducts(
        businessId: String,
        from: String,
        to: String,
        branchId: String?,
        staffId: String?,
        limit: Int = 5
    ): List<TopProductRow>

    // Sales by staff member — for staff performance panel
    @Query("""
    SELECT
        s.staffId                       AS staffId,
        COUNT(s.id)                     AS orderCount,
        SUM(s.totalAmount)              AS revenue
    FROM sales s
    WHERE s.businessId  = :businessId
      AND s.soldAt      BETWEEN :from AND :to
      AND s.status     != 'CANCELLED'
      AND (:branchId    IS NULL OR s.branchId = :branchId)
    GROUP BY s.staffId
    ORDER BY revenue DESC
""")
    suspend fun getSalesByStaff(
        businessId: String,
        from: String,
        to: String,
        branchId: String?
    ): List<StaffSalesRow>

    // Distinct staff IDs who made sales — used to populate the staff filter
    @Query("""
    SELECT DISTINCT staffId FROM sales
    WHERE businessId = :businessId
""")
    suspend fun getDistinctStaffIds(businessId: String): List<String>

    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getSaleByIdOnce(saleId: String): SaleEntity?

    @Query("SELECT COUNT(*) + 1 FROM sales WHERE businessId = :businessId")
    suspend fun getNextSaleCounter(businessId: String): Int

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :saleId")
    fun observeSaleById(saleId: String): Flow<SaleWithItems?>

    @Query("SELECT * FROM sales")
    suspend fun getAllSales(): List<SaleEntity>
}