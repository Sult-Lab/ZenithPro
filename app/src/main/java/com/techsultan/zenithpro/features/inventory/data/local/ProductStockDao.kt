package com.techsultan.zenithpro.features.inventory.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductStockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: List<ProductStockEntity>)

    @Query("SELECT * FROM product_stock WHERE variantId = :variantId")
    suspend fun getStockForVariant(variantId: String): List<ProductStockEntity>

    @Query("SELECT * FROM product_stock WHERE syncStatus IN ('PENDING','DIRTY')")
    suspend fun getUnsyncedStock(): List<ProductStockEntity>

    @Query("UPDATE product_stock SET syncStatus = 'SYNCED', updatedAt = :at WHERE id = :id")
    suspend fun markSynced(id: String, at: String)

    @Query("DELETE FROM product_stock WHERE variantId = :variantId")
    suspend fun deleteForVariant(variantId: String)
}