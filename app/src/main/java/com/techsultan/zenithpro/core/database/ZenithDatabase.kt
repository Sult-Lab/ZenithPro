package com.techsultan.zenithpro.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.techsultan.zenithpro.features.product.data.local.ProductDao
import com.techsultan.zenithpro.features.product.data.local.ProductEntity
import com.techsultan.zenithpro.features.product.data.local.ProductStockDao
import com.techsultan.zenithpro.features.product.data.local.ProductStockEntity
import com.techsultan.zenithpro.features.product.data.local.ProductVariantDao
import com.techsultan.zenithpro.features.product.data.local.ProductVariantEntity
import com.techsultan.zenithpro.features.product.data.local.VariantAttributeEntity
import com.techsultan.zenithpro.features.sales.data.local.SaleDao
import com.techsultan.zenithpro.features.sales.data.local.SaleEntity
import com.techsultan.zenithpro.features.sales.data.local.SaleItemEntity

@Database(
    entities = [
        ProductEntity::class,
        ProductVariantEntity::class,
        ProductStockEntity::class,
        VariantAttributeEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
               ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ZenithDatabase : RoomDatabase() {
    abstract val productDao: ProductDao
    abstract val productVariantDao: ProductVariantDao
    abstract val stockVariantDao: ProductStockDao
    abstract val saleDao: SaleDao

    companion object {
        const val DATABASE_NAME = "zenith_db"
    }
}