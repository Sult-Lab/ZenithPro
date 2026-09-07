package com.techsultan.zenithpro.features.inventory.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.inventory.data.local.ProductAuditLogEntity
import com.techsultan.zenithpro.features.inventory.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow

class GetProductAuditLogUseCase(
    private val repository: ProductRepository
) {
    operator fun invoke(productId: String): Flow<Resource<List<ProductAuditLogEntity>>> {
        return repository.getProductAuditLogs(productId)
    }
}
