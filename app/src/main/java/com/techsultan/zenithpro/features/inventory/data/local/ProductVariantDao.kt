package com.techsultan.zenithpro.features.inventory.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.techsultan.zenithpro.core.util.Util

@Dao
interface ProductVariantDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariant(variant: ProductVariantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariants(variants: List<ProductVariantEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttributes(attrs: List<VariantAttributeEntity>)

    @Query("SELECT * FROM product_variants WHERE productId = :productId AND syncStatus != 'DELETED'")
    suspend fun getVariantsForProduct(productId: String): List<ProductVariantEntity>

    @Query("SELECT * FROM product_variants WHERE syncStatus IN ('PENDING','DIRTY','DELETED')")
    suspend fun getUnsyncedVariants(): List<ProductVariantEntity>

    @Query("UPDATE product_variants SET syncStatus = 'SYNCED', updatedAt = :at WHERE id = :id")
    suspend fun markSynced(id: String, at: String)

    @Query("UPDATE product_variants SET deletedAt = :at, syncStatus = 'DELETED' WHERE id = :id")
    suspend fun softDelete(id: String, at: String)

    @Query("DELETE FROM product_variants WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("DELETE FROM product_variants WHERE productId = :productId")
    suspend fun deleteVariantsForProduct(productId: String)

    @Query("SELECT id FROM product_variants WHERE businessId = :businessId")
    suspend fun getAllVariantIdsForBusiness(businessId: String): List<String>

    @Query("""
    UPDATE product_variants SET
        sku        = :sku,
        salesPrice = :salesPrice,
        costPrice  = :costPrice,
        barcode    = :barcode,
        updatedAt  = :updatedAt,
        syncStatus = :syncStatus
    WHERE id = :id
""")
    suspend fun updateVariant(
        id: String,
        sku: String,
        salesPrice: Long,
        costPrice: Long,
        barcode: String?,
        updatedAt: String,
        syncStatus: Util.SyncStatus
    )

    @Query("DELETE FROM variant_attributes WHERE variantId = :variantId")
    suspend fun deleteAttributesForVariant(variantId: String)

    @Query("DELETE FROM variant_attributes WHERE variantId IN (:variantIds)")
    suspend fun deleteAttributesForVariants(variantIds: List<String>)

}