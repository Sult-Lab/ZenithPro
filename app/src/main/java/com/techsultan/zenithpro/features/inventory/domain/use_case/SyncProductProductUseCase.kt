package com.techsultan.zenithpro.features.inventory.domain.use_case

import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.manager.SyncManager
import com.techsultan.zenithpro.features.inventory.domain.repository.ProductRepository

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