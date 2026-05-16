package com.techsultan.zenithpro.features.category.domain.use_case

import com.techsultan.zenithpro.features.category.domain.repository.CategoryRepository

class SearchCategoryUseCase(private val repository: CategoryRepository){
    operator fun invoke(businessId: String, query: String) = repository.searchCategories(businessId, query)
}