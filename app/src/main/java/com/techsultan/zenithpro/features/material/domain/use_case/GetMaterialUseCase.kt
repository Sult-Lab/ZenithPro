package com.techsultan.zenithpro.features.material.domain.use_case

import com.techsultan.zenithpro.features.material.domain.repository.MaterialRepository

class GetMaterialsUseCase(private val repository: MaterialRepository) {
    operator fun invoke(businessId: String, status: String? = null) =
        repository.getMaterials(businessId, status)
}