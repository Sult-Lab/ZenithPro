package com.techsultan.zenithpro.features.category.domain.use_case

import com.techsultan.zenithpro.features.category.domain.repository.CategoryRepository

class UpsertCategoryUseCase(private val repository: CategoryRepository){
    suspend operator fun invoke(
        id: String?,
        businessId: String,
        color: String?,
        name: String,
        ) = repository.upsertCategory(
        id = id,
        name = name,
        color = color,
        businessId = businessId
    )
}