package com.techsultan.zenithpro.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.techsultan.zenithpro.features.branch.data.local.BranchDao
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.category.data.local.CategoryDao
import com.techsultan.zenithpro.features.category.data.local.CategoryEntity
import com.techsultan.zenithpro.features.customer.data.local.CustomerDao
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseDao
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseEntity
import com.techsultan.zenithpro.features.material.data.local.MaterialDao
import com.techsultan.zenithpro.features.material.data.local.MaterialEntity
import com.techsultan.zenithpro.features.material.data.local.RecipeEntity
import com.techsultan.zenithpro.features.inventory.data.local.ProductDao
import com.techsultan.zenithpro.features.inventory.data.local.ProductEntity
import com.techsultan.zenithpro.features.inventory.data.local.ProductStockDao
import com.techsultan.zenithpro.features.inventory.data.local.ProductStockEntity
import com.techsultan.zenithpro.features.inventory.data.local.ProductVariantDao
import com.techsultan.zenithpro.features.inventory.data.local.ProductVariantEntity
import com.techsultan.zenithpro.features.inventory.data.local.VariantAttributeEntity
import com.techsultan.zenithpro.features.production.data.local.ProductionOrderDao
import com.techsultan.zenithpro.features.production.data.local.ProductionOrderEntity
import com.techsultan.zenithpro.features.sales.data.local.SaleDao
import com.techsultan.zenithpro.features.sales.data.local.SaleEntity
import com.techsultan.zenithpro.features.sales.data.local.SaleItemEntity
import com.techsultan.zenithpro.features.settings.data.local.BusinessSettingsDao
import com.techsultan.zenithpro.features.settings.data.local.BusinessSettingsEntity

@Database(
    entities = [
        ProductEntity::class,
        ProductVariantEntity::class,
        ProductStockEntity::class,
        VariantAttributeEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        CustomerEntity::class,
        ExpenseEntity::class,
        BusinessSettingsEntity::class,
        BranchEntity::class,
        MaterialEntity::class,
        RecipeEntity::class,
        ProductionOrderEntity::class,
        CategoryEntity::class
               ],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ZenithDatabase : RoomDatabase() {
    abstract val productDao: ProductDao
    abstract val productVariantDao: ProductVariantDao
    abstract val stockVariantDao: ProductStockDao
    abstract val saleDao: SaleDao
    abstract val customerDao: CustomerDao
    abstract val expenseDao: ExpenseDao
    abstract val branchDao: BranchDao
    abstract val businessSettingsDao: BusinessSettingsDao
    abstract val materialDao: MaterialDao
    abstract val productionOrderDao: ProductionOrderDao
    abstract val categoryDao: CategoryDao

    companion object {
        const val DATABASE_NAME = "zenith_db"
    }
}
