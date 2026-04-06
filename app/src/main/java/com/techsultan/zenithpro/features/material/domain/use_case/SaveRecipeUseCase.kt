package com.techsultan.zenithpro.features.material.domain.use_case

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.material.data.local.MaterialEntity
import com.techsultan.zenithpro.features.material.data.remote.RecipeItem
import com.techsultan.zenithpro.features.material.data.remote.UpsertMaterialRequest
import com.techsultan.zenithpro.features.material.domain.repository.MaterialRepository

class SaveRecipeUseCase(private val repository: MaterialRepository) {
    suspend operator fun invoke(
        variantId: String, items: List<RecipeItem>, businessId: String
    ): Resource<Unit> {
        if (items.any { it.quantityNeeded <= 0 }) {
            return Resource.Error("All quantities must be greater than zero")
        }
        return repository.saveRecipe(variantId, items, businessId)
    }
}
