package com.techsultan.zenithpro.features.product.domain.use_case

import android.net.Uri
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.manager.SyncManager
import com.techsultan.zenithpro.features.product.data.remote.AddProductRequest
import com.techsultan.zenithpro.features.product.domain.repository.ProductRepository

class SyncProductsUseCase(
    private val repository: ProductRepository,
    private val syncManager: SyncManager,
    private val networkMonitor: NetworkMonitor,
) {
    suspend operator fun invoke(businessId: String) {
        if (!networkMonitor.isConnected()) return
        repository.pullFromServer(businessId)  // pull remote first
        syncManager.syncAll()                  // push any local pending second
    }
}