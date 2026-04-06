package com.techsultan.zenithpro.core.manager

import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseDao
import com.techsultan.zenithpro.features.expenses.data.repository.ExpenseRepositoryImpl
import com.techsultan.zenithpro.features.product.data.local.ProductDao
import com.techsultan.zenithpro.features.product.data.repository.ProductRepositoryImpl

class SyncManager(
    private val repository: ProductRepositoryImpl,
    private val expenseRepository: ExpenseRepositoryImpl,
    private val productDao: ProductDao,
    private val expenseDao: ExpenseDao,
    private val networkMonitor: NetworkMonitor,
) {
    suspend fun syncAll() {
        if (!networkMonitor.isConnected()) return

        productDao.getUnsyncedProducts().forEach { product ->
            when (product.syncStatus) {
                Util.SyncStatus.PENDING -> repository.pushNewProduct(product.id)
                Util.SyncStatus.DIRTY   -> repository.pushUpdate(product)
                Util.SyncStatus.DELETED -> repository.pushDelete(product.id)
                Util.SyncStatus.SYNCED  -> Unit
            }
        }

        expenseDao.getUnsyncedExpenses().forEach { expense ->
            when (expense.syncStatus) {
                Util.SyncStatus.PENDING,
                Util.SyncStatus.DIRTY   -> expenseRepository.pushExpense(expense)
                Util.SyncStatus.DELETED -> expenseRepository.pushDelete(expense.id)
                Util.SyncStatus.SYNCED  -> Unit
            }
        }
    }
}