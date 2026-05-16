package com.techsultan.zenithpro.features.category.domain.use_case

import com.techsultan.zenithpro.features.category.domain.repository.CategoryRepository

class PullCategoriesUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(businessId: String) = repository.pullFromServer(businessId)
}
