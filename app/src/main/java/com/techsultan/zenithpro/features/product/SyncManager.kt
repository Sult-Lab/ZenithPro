package com.techsultan.zenithpro.features.product

import android.util.Log
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.product.data.local.ProductDao
import com.techsultan.zenithpro.features.product.data.remote.AddProductRequest
import com.techsultan.zenithpro.features.product.data.repository.ProductRepositoryImpl

class SyncManager(
    private val repository: ProductRepositoryImpl,
    private val productDao: ProductDao,
    private val networkMonitor: NetworkMonitor,
) {
    suspend fun syncAll() {
        if (!networkMonitor.isConnected()) return

        productDao.getUnsyncedProducts().forEach { product ->
            when (product.syncStatus) {
                Util.SyncStatus.PENDING -> repository.pushNewProduct(product.id, emptyList(),
                    // Reconstruct minimal request from local data; images already uploaded
                    AddProductRequest(
                        clientId = product.id,
                        name = product.name,
                        description = product.description,
                        category = product.category,
                        baseSalesPrice = product.baseSalesPrice,
                        baseCostPrice = product.baseCostPrice,
                        expiryWarningDays = product.expiryWarningDays,
                        isActive = product.isActive,
                        businessId = product.businessId,
                        imageUrls = product.imageUrls
                    )
                )
                Util.SyncStatus.DELETED -> repository.pushDelete(product.id)
                Util.SyncStatus.DIRTY   -> { /* push update — implement analogously */ }
                Util.SyncStatus.SYNCED  -> Unit
            }
        }
    }
}