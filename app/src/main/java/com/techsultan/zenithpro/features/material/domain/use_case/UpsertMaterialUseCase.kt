package com.techsultan.zenithpro.features.material.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.material.data.local.MaterialEntity
import com.techsultan.zenithpro.features.material.data.remote.UpsertMaterialRequest
import com.techsultan.zenithpro.features.material.domain.repository.MaterialRepository

class UpsertMaterialUseCase(private val repository: MaterialRepository) {
    suspend operator fun invoke(
        request: UpsertMaterialRequest, businessId: String
    ): Resource<MaterialEntity> {
        if (request.name.isBlank()) return Resource.Error("Name is required")
        if (request.costPerUnit < 0) return Resource.Error("Invalid cost")
        return repository.upsertMaterial(request, businessId)
    }
}
