package com.techsultan.zenithpro.core.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
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

        return try {
            // 1. Products
            productDao.getUnsyncedProducts().forEach { product ->
                when (product.syncStatus) {
                    Util.SyncStatus.PENDING -> productRepository.pushNewProduct(product.id)
                    Util.SyncStatus.DIRTY -> productRepository.pushUpdate(product)
                    Util.SyncStatus.DELETED -> productRepository.pushDelete(product.id)
                    Util.SyncStatus.SYNCED -> Unit
                }
            }

            // 2. Expenses
            expenseDao.getUnsyncedExpenses().forEach { expense ->
                when (expense.syncStatus) {
                    Util.SyncStatus.PENDING,
                    Util.SyncStatus.DIRTY -> expenseRepository.pushExpense(expense)
                    Util.SyncStatus.DELETED -> expenseRepository.pushDelete(expense.id)
                    Util.SyncStatus.SYNCED -> Unit
                }
            }

            // 3. Categories
            categoryDao.getUnsyncedCategories().forEach { category ->
                when (category.syncStatus) {
                    Util.SyncStatus.PENDING,
                    Util.SyncStatus.DIRTY -> categoryRepository.pushCategory(category)
                    Util.SyncStatus.DELETED -> categoryRepository.pushDelete(category.id)
                    Util.SyncStatus.SYNCED -> Unit
                }
            }

            // 4. Branches
            branchDao.getUnsyncedBranches().forEach { branch ->
                when (branch.syncStatus) {
                    Util.SyncStatus.PENDING,
                    Util.SyncStatus.DIRTY -> branchRepository.pushBranch(branch)
                    Util.SyncStatus.DELETED -> branchRepository.pushDelete(branch.id)
                    Util.SyncStatus.SYNCED -> Unit
                }
            }

            // 5. Customers
            customerDao.getUnsyncedCustomers().forEach { customer ->
                when (customer.syncStatus) {
                    Util.SyncStatus.PENDING,
                    Util.SyncStatus.DIRTY -> customerRepository.pushCustomer(customer)
                    Util.SyncStatus.DELETED -> customerRepository.deleteCustomer(customer.id)
                    Util.SyncStatus.SYNCED -> Unit
                }
            }

            // 6. Sales
            saleDao.getUnsyncedSales().forEach { sale ->
                // Sale sync is mostly PENDING (new sales)
                if (sale.syncStatus == Util.SyncStatus.PENDING) {
                    saleRepository.pushSale(sale.id)
                }
            }

            // 7. Production Orders
            productionDao.getUnsyncedOrders().forEach { order ->
                when (order.syncStatus) {
                    Util.SyncStatus.PENDING,
                    Util.SyncStatus.DIRTY -> productionRepository.pushOrder(order)
                    else -> Unit
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
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
