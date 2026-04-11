package com.techsultan.zenithpro.features.expenses.data.repository

import android.util.Log
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.expenses.data.local.CategoryBreakdown
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseDao
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseEntity
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseFilter
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseSummary
import com.techsultan.zenithpro.features.expenses.data.mapper.toDto
import com.techsultan.zenithpro.features.expenses.data.mapper.toEntity
import com.techsultan.zenithpro.features.expenses.data.remote.ExpenseDto
import com.techsultan.zenithpro.features.expenses.data.remote.UpsertExpenseRequest
import com.techsultan.zenithpro.features.expenses.domain.repository.ExpenseRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Objects.isNull

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao,
    private val postgrest: Postgrest,
    private val networkMonitor: NetworkMonitor,
) : ExpenseRepository {



    override fun getExpenses(
        businessId: String,
        filter: ExpenseFilter
    ): Flow<Resource<List<ExpenseEntity>>> =
        expenseDao.getExpenses(
            businessId = businessId,
            from       = filter.from.toString(),
            to         = filter.to.toString(),
            category   = filter.category,
            minAmount  = filter.minAmount,
            maxAmount  = filter.maxAmount
        )
            .map<List<ExpenseEntity>, Resource<List<ExpenseEntity>>> { Resource.Success(it) }
            .catch { emit(Resource.Error(it.message ?: "Failed to load expenses")) }
            .onStart { emit(Resource.Loading()) }

    override suspend fun upsertExpense(
        request: UpsertExpenseRequest,
        businessId: String,
        staffId: String
    ): Resource<ExpenseEntity> = withContext(Dispatchers.IO) {
        try {
            val now = Instant.now().toString()
            val existing = expenseDao.getExpenseById(request.id)

            val entity = ExpenseEntity(
                id          = request.id,
                businessId  = businessId,
                branchId    = request.branchId,
                recordedBy  = staffId,
                title       = request.title,
                amount      = request.amount,
                category    = request.category,
                notes       = request.notes,
                receiptUrl  = existing?.receiptUrl,
                expenseDate = request.expenseDate,
                createdAt   = existing?.createdAt ?: now,
                updatedAt   = now,
                deletedAt   = null,
                syncStatus  = Util.SyncStatus.PENDING
            )

            // 1. Save locally first
            expenseDao.insertExpense(entity)

            // 2. Push to server if online
            if (networkMonitor.isConnected()) {
                pushExpense(entity)
            }

            Resource.Success(entity)
        } catch (e: Exception) {
            Log.e("ExpenseRepo", "upsertExpense: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to save expense")
        }
    }

    override suspend fun deleteExpense(expenseId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                expenseDao.softDelete(expenseId, Instant.now().toString())
                if (networkMonitor.isConnected()) {
                    pushDelete(expenseId)
                }
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to delete expense")
            }
        }


    override suspend fun getCategories(businessId: String): List<String> =
        expenseDao.getCategories(businessId)

    override suspend fun getSummary(
        businessId: String,
        filter: ExpenseFilter
    ): Resource<ExpenseSummary> = withContext(Dispatchers.IO) {
        try {
            Resource.Success(
                expenseDao.getSummary(
                    businessId = businessId,
                    from       = filter.from.toString(),
                    to         = filter.to.toString(),
                    category   = filter.category
                )
            )
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load summary")
        }
    }

    override suspend fun getCategoryBreakdown(
        businessId: String,
        filter: ExpenseFilter
    ): Resource<List<CategoryBreakdown>> = withContext(Dispatchers.IO) {
        try {
            Resource.Success(
                expenseDao.getCategoryBreakdown(
                    businessId = businessId,
                    from       = filter.from.toString(),
                    to         = filter.to.toString()
                )
            )
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load breakdown")
        }
    }

    override suspend fun pullFromServer(businessId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            if (businessId.isBlank()) {
                Log.e("ExpenseRepo", "pullFromServer: businessId is blank")
                return@withContext Resource.Error("Business ID is missing")
            }
            try {
                val remote = postgrest
                    .from("expenses")
                    .select {
                        filter {
                            eq("business_id", businessId)
                            isNull("deleted_at")
                        }
                        order("expense_date", Order.DESCENDING)
                        limit(500)
                    }
                    .decodeList<ExpenseDto>()

                val unsyncedIds = expenseDao.getUnsyncedExpenses()
                    .map { it.id }.toSet()

                val toUpsert = remote
                    .filter { it.id !in unsyncedIds }
                    .map { it.toEntity() }

                if (toUpsert.isNotEmpty()) {
                    expenseDao.insertExpenses(toUpsert)
                }

                // Remove server-deleted records locally
                val remoteIds = remote.map { it.id }.toSet()
                val localIds  = expenseDao.getAllExpenseIds(businessId).toSet()
                localIds
                    .filter { it !in remoteIds && it !in unsyncedIds }
                    .forEach { expenseDao.hardDelete(it) }

                Resource.Success(Unit)
            } catch (e: Exception) {
                Log.e("ExpenseRepo", "pullFromServer error: ${e.message}", e)
                Resource.Error(e.message ?: "Pull failed")
            }
        }

    internal suspend fun pushExpense(entity: ExpenseEntity) {
        try {
            postgrest.from("expenses").upsert(entity.toDto()) {
                onConflict = "id"
            }
            expenseDao.markSynced(entity.id, Instant.now().toString())
            Log.d("ExpenseRepo", "pushExpense: synced ${entity.id}")
        } catch (e: Exception) {
            Log.w("ExpenseRepo", "pushExpense failed for ${entity.id}: ${e.message}")
        }
    }

    internal suspend fun pushDelete(expenseId: String) {
        try {
            postgrest.from("expenses")
                .update(mapOf("deleted_at" to Instant.now().toString())) {
                    filter { eq("id", expenseId) }
                }
            expenseDao.hardDelete(expenseId)
        } catch (e: Exception) {
            Log.w("ExpenseRepo", "pushDelete failed for $expenseId: ${e.message}")
        }
    }
}
