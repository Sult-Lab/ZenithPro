package com.techsultan.zenithpro.features.inventory.data.local

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

    @Query("SELECT 0")
    suspend fun getPendingPurchaseOrderCount(): Int

    // ProductDao.kt — add these
    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("""
    UPDATE products SET
        name              = :name,
        description       = :description,
        category          = :category,
        baseSalesPrice    = :baseSalesPrice,
        baseCostPrice     = :baseCostPrice,
        expiryWarningDays = :expiryWarningDays,
        isActive          = :isActive,
        imageUrls         = :imageUrls,
        updatedAt         = :updatedAt,
        syncStatus        = :syncStatus
    WHERE id = :id
""")
    suspend fun updateProduct(
        id: String,
        name: String,
        description: String?,
        category: String?,
        baseSalesPrice: Long,
        baseCostPrice: Long,
        expiryWarningDays: Int?,
        isActive: Boolean,
        imageUrls: List<String>,
        updatedAt: String,
        syncStatus: Util.SyncStatus
    )

    // ProductVariantDao.kt — add these
    @Query("SELECT * FROM product_variants WHERE productId = :productId AND syncStatus != 'DELETED'")
    suspend fun getVariantsForProduct(productId: String): List<ProductVariantEntity>

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

    @Query("""
    SELECT COUNT(DISTINCT pv.id)
    FROM product_variants pv
    INNER JOIN product_stock ps ON ps.variantId = pv.id
    WHERE pv.businessId = :businessId
      AND (:branchId IS NULL OR ps.branchId = :branchId)
      AND pv.deletedAt IS NULL
      AND pv.syncStatus != 'DELETED'
      AND ps.lowStockAlert IS NOT NULL
      AND ps.quantity <= ps.lowStockAlert
""")
    suspend fun getLowStockCount(businessId: String, branchId: String?): Int

    @Transaction
    @Query("""
    SELECT DISTINCT p.* FROM products p
    INNER JOIN product_variants pv ON pv.productId = p.id
    INNER JOIN product_stock ps ON ps.variantId = pv.id
    WHERE p.businessId = :businessId
      AND ps.branchId = :branchId
      AND p.deletedAt IS NULL
      AND p.syncStatus != 'DELETED'
    ORDER BY 
        CASE WHEN p.syncStatus = 'PENDING' THEN 0 ELSE 1 END,
        p.updatedAt DESC
""")
    fun getProductsForBranch(businessId: String, branchId: String): Flow<List<ProductWithVariants>>

}