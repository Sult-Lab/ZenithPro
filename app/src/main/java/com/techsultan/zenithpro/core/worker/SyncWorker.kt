package com.techsultan.zenithpro.core.worker

import android.content.Context
import android.util.Log
import android.os.SystemClock
import androidx.core.os.bundleOf
import androidx.work.Constraints
import com.techsultan.zenithpro.core.util.ZenithAnalytics
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.branch.data.local.BranchDao
import com.techsultan.zenithpro.features.branch.data.repository.BranchRepositoryImpl
import com.techsultan.zenithpro.features.category.data.local.CategoryDao
import com.techsultan.zenithpro.features.category.data.repository.CategoryRepositoryImpl
import com.techsultan.zenithpro.features.customer.data.local.CustomerDao
import com.techsultan.zenithpro.features.customer.data.repository.CustomerRepositoryImpl
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseDao
import com.techsultan.zenithpro.features.expenses.data.repository.ExpenseRepositoryImpl
import com.techsultan.zenithpro.features.inventory.data.local.ProductDao
import com.techsultan.zenithpro.features.inventory.data.repository.ProductRepositoryImpl
import com.techsultan.zenithpro.features.production.data.local.ProductionOrderDao
import com.techsultan.zenithpro.features.production.data.repository.ProductionRepositoryImpl
import com.techsultan.zenithpro.features.sales.data.local.SaleDao
import com.techsultan.zenithpro.features.sales.data.repository.SaleRepositoryImpl
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val productRepository: ProductRepositoryImpl,
    private val expenseRepository: ExpenseRepositoryImpl,
    private val categoryRepository: CategoryRepositoryImpl,
    private val branchRepository: BranchRepositoryImpl,
    private val customerRepository: CustomerRepositoryImpl,
    private val saleRepository: SaleRepositoryImpl,
    private val productionRepository: ProductionRepositoryImpl,
    private val productDao: ProductDao,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val branchDao: BranchDao,
    private val customerDao: CustomerDao,
    private val saleDao: SaleDao,
    private val productionDao: ProductionOrderDao,
    private val networkMonitor: NetworkMonitor,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!networkMonitor.isConnected()) return Result.retry()

        val startTime = SystemClock.elapsedRealtime()
        ZenithAnalytics.trackEvent("sync_started", bundleOf("trigger" to "auto"))
        var totalSynced = 0

        return try {
            // 1. Products
            val products = productDao.getUnsyncedProducts()
            products.forEach { product ->
                try {
                    when (product.syncStatus) {
                        Util.SyncStatus.PENDING -> productRepository.pushNewProduct(product.id)
                        Util.SyncStatus.DIRTY -> productRepository.pushUpdate(product)
                        Util.SyncStatus.DELETED -> productRepository.pushDelete(product.id)
                        Util.SyncStatus.SYNCED -> Unit
                    }
                    totalSynced++
                } catch (e: Exception) {
                    ZenithAnalytics.logError(e, context = "SyncWorker.Products")
                    ZenithAnalytics.trackEvent("sync_failed", bundleOf(
                        "entity_type" to "product",
                        "error_reason" to (e.message ?: "unknown")
                    ))
                }
            }

            // 2. Expenses
            val expenses = expenseDao.getUnsyncedExpenses()
            expenses.forEach { expense ->
                try {
                    when (expense.syncStatus) {
                        Util.SyncStatus.PENDING,
                        Util.SyncStatus.DIRTY -> expenseRepository.pushExpense(expense)
                        Util.SyncStatus.DELETED -> expenseRepository.pushDelete(expense.id)
                        Util.SyncStatus.SYNCED -> Unit
                    }
                    totalSynced++
                } catch (e: Exception) {
                    ZenithAnalytics.logError(e, context = "SyncWorker.Expenses")
                    ZenithAnalytics.trackEvent("sync_failed", bundleOf(
                        "entity_type" to "expense",
                        "error_reason" to (e.message ?: "unknown")
                    ))
                }
            }

            // 3. Categories
            val categories = categoryDao.getUnsyncedCategories()
            categories.forEach { category ->
                try {
                    when (category.syncStatus) {
                        Util.SyncStatus.PENDING,
                        Util.SyncStatus.DIRTY -> categoryRepository.pushCategory(category)
                        Util.SyncStatus.DELETED -> categoryRepository.pushDelete(category.id)
                        Util.SyncStatus.SYNCED -> Unit
                    }
                    totalSynced++
                } catch (e: Exception) {
                    ZenithAnalytics.logError(e, context = "SyncWorker.Categories")
                    ZenithAnalytics.trackEvent("sync_failed", bundleOf(
                        "entity_type" to "category",
                        "error_reason" to (e.message ?: "unknown")
                    ))
                }
            }

            // 4. Branches
            val branches = branchDao.getUnsyncedBranches()
            branches.forEach { branch ->
                try {
                    when (branch.syncStatus) {
                        Util.SyncStatus.PENDING,
                        Util.SyncStatus.DIRTY -> branchRepository.pushBranch(branch)
                        Util.SyncStatus.DELETED -> branchRepository.pushDelete(branch.id)
                        Util.SyncStatus.SYNCED -> Unit
                    }
                    totalSynced++
                } catch (e: Exception) {
                    ZenithAnalytics.logError(e, context = "SyncWorker.Branches")
                    ZenithAnalytics.trackEvent("sync_failed", bundleOf(
                        "entity_type" to "branch",
                        "error_reason" to (e.message ?: "unknown")
                    ))
                }
            }

            // 5. Customers
            val customers = customerDao.getUnsyncedCustomers()
            customers.forEach { customer ->
                try {
                    when (customer.syncStatus) {
                        Util.SyncStatus.PENDING,
                        Util.SyncStatus.DIRTY -> customerRepository.pushCustomer(customer)
                        Util.SyncStatus.DELETED -> customerRepository.deleteCustomer(customer.id)
                        Util.SyncStatus.SYNCED -> Unit
                    }
                    totalSynced++
                } catch (e: Exception) {
                    ZenithAnalytics.logError(e, context = "SyncWorker.Customers")
                    ZenithAnalytics.trackEvent("sync_failed", bundleOf(
                        "entity_type" to "customer",
                        "error_reason" to (e.message ?: "unknown")
                    ))
                }
            }

            // 6. Sales
            val sales = saleDao.getUnsyncedSales()
            sales.forEach { sale ->
                try {
                    // Sale sync is mostly PENDING (new sales)
                    if (sale.syncStatus == Util.SyncStatus.PENDING) {
                        saleRepository.pushSale(sale.id)
                    }
                    totalSynced++
                } catch (e: Exception) {
                    ZenithAnalytics.logError(e, context = "SyncWorker.Sales")
                    ZenithAnalytics.trackEvent("sync_failed", bundleOf(
                        "entity_type" to "sale",
                        "error_reason" to (e.message ?: "unknown")
                    ))
                }
            }

            // 7. Production Orders
            val orders = productionDao.getUnsyncedOrders()
            orders.forEach { order ->
                try {
                    when (order.syncStatus) {
                        Util.SyncStatus.PENDING,
                        Util.SyncStatus.DIRTY -> productionRepository.pushOrder(order)
                        else -> Unit
                    }
                    totalSynced++
                } catch (e: Exception) {
                    ZenithAnalytics.logError(e, context = "SyncWorker.ProductionOrders")
                    ZenithAnalytics.trackEvent("sync_failed", bundleOf(
                        "entity_type" to "production_order",
                        "error_reason" to (e.message ?: "unknown")
                    ))
                }
            }

            val duration = SystemClock.elapsedRealtime() - startTime
            ZenithAnalytics.trackEvent("sync_completed", bundleOf(
                "duration_ms" to duration,
                "entities_synced" to totalSynced
            ))
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            ZenithAnalytics.logError(e, context = "SyncWorker.doWork")
            ZenithAnalytics.trackEvent("sync_failed", bundleOf(
                "error_reason" to (e.message ?: "unknown")
            ))
            Result.retry()
        }
    }

    companion object {
        private const val SYNC_WORK_NAME = "periodic_sync_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
