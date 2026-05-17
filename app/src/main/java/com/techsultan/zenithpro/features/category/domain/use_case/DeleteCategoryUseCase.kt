package com.techsultan.zenithpro.features.category.domain.use_case

import com.techsultan.zenithpro.features.category.domain.repository.CategoryRepository

class DeleteCategoryUseCase(private val repository: CategoryRepository){
    suspend operator fun invoke(categoryId: String) = repository.deleteCategory(categoryId)
}