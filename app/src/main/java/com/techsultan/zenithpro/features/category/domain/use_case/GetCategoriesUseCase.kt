package com.techsultan.zenithpro.features.category.domain.use_case

import com.techsultan.zenithpro.features.category.domain.repository.CategoryRepository

class GetCategoriesUseCase(private val repository: CategoryRepository){
    operator fun invoke(businessId: String) = repository.getCategories(businessId)
}