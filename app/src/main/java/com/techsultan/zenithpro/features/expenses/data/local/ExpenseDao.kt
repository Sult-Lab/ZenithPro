package com.techsultan.zenithpro.features.expenses.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    // ── Observe with filters ───────────────────────────────────────
    @Query("""
        SELECT * FROM expenses
        WHERE businessId  = :businessId
          AND deletedAt   IS NULL
          AND syncStatus  != 'DELETED'
          AND expenseDate BETWEEN :from AND :to
          AND (:category  IS NULL OR category = :category)
          AND (:minAmount IS NULL OR amount >= :minAmount)
          AND (:maxAmount IS NULL OR amount <= :maxAmount)
        ORDER BY expenseDate DESC, createdAt DESC
    """)
    fun getExpenses(
        businessId: String,
        from: String,
        to: String,
        category: String?,
        minAmount: Long?,
        maxAmount: Long?
    ): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: String): ExpenseEntity?

    // ── Category suggestions from existing data ────────────────────
    @Query("""
        SELECT DISTINCT category FROM expenses
        WHERE businessId = :businessId
          AND deletedAt  IS NULL
        ORDER BY category ASC
    """)
    suspend fun getCategories(businessId: String): List<String>

    // ── Summary ────────────────────────────────────────────────────
    @Query("""
        SELECT
            COALESCE(SUM(amount), 0) AS totalAmount,
            COUNT(*)                 AS totalCount
        FROM expenses
        WHERE businessId  = :businessId
          AND deletedAt   IS NULL
          AND expenseDate BETWEEN :from AND :to
          AND (:category  IS NULL OR category = :category)
    """)
    suspend fun getSummary(
        businessId: String,
        from: String,
        to: String,
        category: String?
    ): ExpenseSummary

    // ── Category breakdown ─────────────────────────────────────────
    @Query("""
        SELECT
            category,
            SUM(amount)  AS totalAmount,
            COUNT(*)     AS count
        FROM expenses
        WHERE businessId  = :businessId
          AND deletedAt   IS NULL
          AND expenseDate BETWEEN :from AND :to
        GROUP BY category
        ORDER BY totalAmount DESC
    """)
    suspend fun getCategoryBreakdown(
        businessId: String,
        from: String,
        to: String
    ): List<CategoryBreakdown>

    // ── Sync helpers ───────────────────────────────────────────────
    @Query("SELECT * FROM expenses WHERE syncStatus IN ('PENDING','DIRTY','DELETED')")
    suspend fun getUnsyncedExpenses(): List<ExpenseEntity>

    @Query("UPDATE expenses SET syncStatus = 'SYNCED', updatedAt = :at WHERE id = :id")
    suspend fun markSynced(id: String, at: String)

    @Query("UPDATE expenses SET deletedAt = :at, syncStatus = 'DELETED' WHERE id = :id")
    suspend fun softDelete(id: String, at: String)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT id FROM expenses WHERE businessId = :businessId")
    suspend fun getAllExpenseIds(businessId: String): List<String>
}