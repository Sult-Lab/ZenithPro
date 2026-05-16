package com.techsultan.zenithpro.features.category.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("""
        SELECT * FROM categories
        WHERE businessId = :businessId
          AND deletedAt  IS NULL
          AND syncStatus != 'DELETED'
        ORDER BY name ASC
    """)
    fun getCategories(businessId: String): Flow<List<CategoryEntity>>

    @Query("""
        SELECT * FROM categories
        WHERE businessId = :businessId
          AND deletedAt  IS NULL
          AND name LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchCategories(businessId: String, query: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE syncStatus IN ('PENDING','DIRTY','DELETED')")
    suspend fun getUnsyncedCategories(): List<CategoryEntity>

    @Query("UPDATE categories SET syncStatus = 'SYNCED', updatedAt = :at WHERE id = :id")
    suspend fun markSynced(id: String, at: String)

    @Query("UPDATE categories SET deletedAt = :at, syncStatus = 'DELETED' WHERE id = :id")
    suspend fun softDelete(id: String, at: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT id FROM categories WHERE businessId = :businessId")
    suspend fun getAllCategoryIds(businessId: String): List<String>
}