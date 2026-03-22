package com.techsultan.zenithpro.features.product.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.techsultan.zenithpro.core.util.Util
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Transaction
    @Query("SELECT * FROM products WHERE deletedAt IS NULL AND syncStatus != 'DELETED'")
    fun getAllProductsWithVariants(): Flow<List<ProductWithVariants>>

    @Transaction
    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductWithVariants(id: String): ProductWithVariants?

    @Query("SELECT * FROM products WHERE syncStatus IN ('PENDING','DIRTY','DELETED')")
    suspend fun getUnsyncedProducts(): List<ProductEntity>

    @Query("UPDATE products SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: Util.SyncStatus)

    @Query("UPDATE products SET syncStatus = 'SYNCED', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSynced(id: String, updatedAt: String)

    @Query("UPDATE products SET deletedAt = :at, syncStatus = 'DELETED' WHERE id = :id")
    suspend fun softDelete(id: String, at: String)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("DELETE FROM products")
    suspend fun deleteAll()

    @Transaction
    @Query("""
        SELECT * FROM products 
        WHERE businessId = :businessId 
          AND deletedAt IS NULL 
          AND syncStatus != 'DELETED'
        ORDER BY 
          CASE WHEN syncStatus = 'PENDING' THEN 0 ELSE 1 END, 
          updatedAt DESC
    """)
    fun getProductsForBusiness(businessId: String): Flow<List<ProductWithVariants>>

    @Query("SELECT id FROM products WHERE businessId = :businessId")
    suspend fun getAllProductIds(businessId: String): List<String>
}