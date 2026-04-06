package com.techsultan.zenithpro.features.material.domain.repository

import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.material.data.local.MaterialEntity
import com.techsultan.zenithpro.features.material.data.remote.RecipeItem
import com.techsultan.zenithpro.features.material.data.local.RecipeWithMaterial
import com.techsultan.zenithpro.features.material.data.remote.UpsertMaterialRequest
import kotlinx.coroutines.flow.Flow

interface MaterialRepository {
    fun getMaterials(businessId: String, status: String? = null): Flow<Resource<List<MaterialEntity>>>
    fun getLowStockMaterials(businessId: String): Flow<Resource<List<MaterialEntity>>>
    fun getRecipeForVariant(variantId: String): Flow<Resource<List<RecipeWithMaterial>>>
    suspend fun upsertMaterial(request: UpsertMaterialRequest, businessId: String): Resource<MaterialEntity>
    suspend fun updateStatus(materialId: String, status: String): Resource<Unit>
    suspend fun adjustStock(materialId: String, quantity: Double, notes: String?, staffId: String, businessId: String): Resource<Unit>
    suspend fun saveRecipe(variantId: String, items: List<RecipeItem>, businessId: String): Resource<Unit>
    suspend fun deleteMaterial(materialId: String): Resource<Unit>
    suspend fun getLowStockCount(businessId: String): Int
    suspend fun pullFromServer(businessId: String): Resource<Unit>
}