package com.techsultan.zenithpro.features.category.domain.repository

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.category.data.local.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(businessId: String): Flow<Resource<List<CategoryEntity>>>
    fun searchCategories(businessId: String, query: String): Flow<List<CategoryEntity>>
    suspend fun upsertCategory(
        id: String?,
        name: String,
        color: String?,
        businessId: String
    ): Resource<CategoryEntity>
    suspend fun deleteCategory(categoryId: String): Resource<Unit>
    suspend fun pullFromServer(businessId: String): Resource<Unit>
}