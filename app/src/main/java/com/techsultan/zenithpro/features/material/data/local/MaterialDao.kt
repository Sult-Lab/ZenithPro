package com.techsultan.zenithpro.features.material.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: MaterialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterials(materials: List<MaterialEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<RecipeEntity>)

    @Query("""
        SELECT * FROM materials
        WHERE businessId = :businessId
          AND deletedAt  IS NULL
          AND syncStatus != 'DELETED'
          AND (:status IS NULL OR status = :status)
        ORDER BY name ASC
    """)
    fun getMaterials(businessId: String, status: String?): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials WHERE id = :id")
    suspend fun getMaterialById(id: String): MaterialEntity?

    @Transaction
    @Query("SELECT * FROM recipes WHERE variantId = :variantId")
    fun getRecipeForVariant(variantId: String): Flow<List<RecipeWithMaterial>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE businessId = :businessId")
    fun getAllRecipes(businessId: String): Flow<List<RecipeWithMaterial>>

    @Query("SELECT * FROM recipes WHERE variantId = :variantId")
    suspend fun getRecipesByVariant(variantId: String): List<RecipeEntity>

    @Query("DELETE FROM recipes WHERE variantId = :variantId")
    suspend fun deleteRecipeForVariant(variantId: String)

    @Query("""
        SELECT * FROM materials
        WHERE businessId   = :businessId
          AND deletedAt    IS NULL
          AND lowStockAlert IS NOT NULL
          AND quantity      <= lowStockAlert
    """)
    fun getLowStockMaterials(businessId: String): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials WHERE syncStatus IN ('PENDING','DIRTY','DELETED')")
    suspend fun getUnsyncedMaterials(): List<MaterialEntity>

    @Query("UPDATE materials SET syncStatus = 'SYNCED', updatedAt = :at WHERE id = :id")
    suspend fun markSynced(id: String, at: String)

    @Query("UPDATE materials SET deletedAt = :at, syncStatus = 'DELETED' WHERE id = :id")
    suspend fun softDelete(id: String, at: String)

    @Query("DELETE FROM materials WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT id FROM materials")
    suspend fun getAllMaterialIds(): List<String>

    @Query("SELECT id FROM materials WHERE businessId = :businessId")
    suspend fun getAllMaterialIds(businessId: String): List<String>

    @Query("""
        SELECT COUNT(*) FROM materials
        WHERE businessId   = :businessId
          AND deletedAt    IS NULL
          AND lowStockAlert IS NOT NULL
          AND quantity      <= lowStockAlert
    """)
    suspend fun getLowStockCount(businessId: String): Int

     @Query("SELECT * FROM recipes WHERE variantId = :variantId")
 suspend fun getRecipeEntitiesForVariantOnce(variantId: String): List<RecipeEntity>
     // one-shot, unfiltered (DELETED included) — used only for diffing in saveRecipe

    @Query("SELECT * FROM recipes WHERE syncStatus IN ('PENDING','DIRTY','DELETED')")
 suspend fun getUnsyncedRecipes(): List<RecipeEntity>
     // feeds SyncWorker

 @Query("UPDATE recipes SET syncStatus = 'SYNCED', updatedAt = :syncedAt WHERE id = :id")
 suspend fun markRecipeSynced(id: String, syncedAt: String)

 @Query("DELETE FROM recipes WHERE id = :id")
 suspend fun hardDeleteRecipe(id: String)

    @Query("""
    SELECT * FROM recipes
    WHERE businessId = :businessId
""")
    suspend fun getRecipesForBusiness(
        businessId: String
    ): List<RecipeEntity>

    @Query("""
    SELECT * FROM recipes
    WHERE id = :id
    LIMIT 1
""")
    suspend fun getRecipeById(
        id: String
    ): RecipeEntity?

}