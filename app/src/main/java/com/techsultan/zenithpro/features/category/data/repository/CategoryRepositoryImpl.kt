package com.techsultan.zenithpro.features.category.data.repository

import android.util.Log
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.category.data.local.CategoryDao
import com.techsultan.zenithpro.features.category.data.local.CategoryEntity
import com.techsultan.zenithpro.features.category.data.mapper.toDto
import com.techsultan.zenithpro.features.category.data.mapper.toEntity
import com.techsultan.zenithpro.features.category.data.remote.CategoryDto
import com.techsultan.zenithpro.features.category.domain.repository.CategoryRepository
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Objects.isNull
import java.util.UUID

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val postgrest: Postgrest,
    private val networkMonitor: NetworkMonitor,
) : CategoryRepository {

    override fun getCategories(businessId: String) =
        categoryDao.getCategories(businessId)
            .map<List<CategoryEntity>, Resource<List<CategoryEntity>>> { Resource.Success(it) }
            .catch { emit(Resource.Error(it.message ?: "Failed")) }
            .onStart { emit(Resource.Loading()) }

    override fun searchCategories(businessId: String, query: String) =
        categoryDao.searchCategories(businessId, query)

    override suspend fun upsertCategory(
        id: String?,
        name: String,
        color: String?,
        businessId: String
    ): Resource<CategoryEntity> = withContext(Dispatchers.IO) {
        try {
            val categoryId = id ?: UUID.randomUUID().toString()
            val now        = Instant.now().toString()
            val existing   = id?.let { categoryDao.getCategoryById(it) }

            val entity = CategoryEntity(
                id         = categoryId,
                businessId = businessId,
                name       = name.trim(),
                color      = color,
                icon       = null,
                createdAt  = existing?.createdAt ?: now,
                updatedAt  = now,
                deletedAt  = null,
                syncStatus = Util.SyncStatus.PENDING
            )

            categoryDao.insertCategory(entity)

            if (networkMonitor.isConnected()) {
                Log.d("CategoryRepo", "Upserting category to server")
                postgrest.from("categories").upsert(entity.toDto()) {
                    onConflict = "id"
                }
                Log.d("CategoryRepo", "Upserting category to server")
                categoryDao.markSynced(categoryId, now)
            }
            Log.d("CategoryRepo", "Upserting category to server: $entity")
            Resource.Success(entity)
        } catch (e: Exception) {
            if (e.message?.contains("unique") == true ||
                e.message?.contains("duplicate") == true) {
                Resource.Error("A category with this name already exists")
            } else {
                Resource.Error(e.message ?: "Failed to save category")
            }
        }
    }

    override suspend fun deleteCategory(categoryId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                categoryDao.softDelete(categoryId, Instant.now().toString())
                if (networkMonitor.isConnected()) {
                    postgrest.from("categories")
                        .update(mapOf("deleted_at" to Instant.now().toString())) {
                            filter { eq("id", categoryId) }
                        }
                    categoryDao.hardDelete(categoryId)
                }
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to delete category")
            }
        }

    override suspend fun pullFromServer(businessId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val remote = postgrest.from("categories").select {
                    filter {
                        eq("business_id", businessId)
                        isNull("deleted_at")
                    }
                }.decodeList<CategoryDto>()
                Log.d("CategoryRepo", "Pulling categories from server: $remote")
                val unsyncedIds = categoryDao.getUnsyncedCategories().map { it.id }.toSet()
                Log.d("CategoryRepo", "Unsynced IDs: $unsyncedIds")
                remote.filter { it.id !in unsyncedIds }
                    .map { it.toEntity() }
                    .let { if (it.isNotEmpty()) categoryDao.insertCategories(it) }
                Log.d("CategoryRepo", "Inserted categories: ${remote.map { it.toEntity() }}")
                val remoteIds = remote.map { it.id }.toSet()
                Log.d("CategoryRepo", "Remote IDs: $remoteIds")
                categoryDao.getAllCategoryIds(businessId)
                    .filter { it !in remoteIds && it !in unsyncedIds }
                    .forEach { categoryDao.hardDelete(it) }

                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Pull failed")
            }
        }
}