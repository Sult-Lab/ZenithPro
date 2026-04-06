package com.techsultan.zenithpro.features.material.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.material.data.local.MaterialEntity
import com.techsultan.zenithpro.features.material.data.remote.UpsertMaterialRequest
import com.techsultan.zenithpro.features.material.domain.repository.MaterialRepository

class AdjustMaterialStockUseCase(private val repository: MaterialRepository) {
    suspend operator fun invoke(
        materialId: String, quantity: Double,
        notes: String?, staffId: String, businessId: String
    ): Resource<Unit> {
        if (quantity == 0.0) return Resource.Error("Quantity cannot be zero")
        return repository.adjustStock(materialId, quantity, notes, staffId, businessId)
    }
}
