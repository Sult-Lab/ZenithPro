package com.techsultan.zenithpro.core.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.techsultan.zenithpro.core.database.ZenithDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val MIGRATION_1_2 = object : Migration(1,2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `production_orders` ADD COLUMN `outcome` TEXT NOT NULL DEFAULT 'STOCK'")
        db.execSQL("ALTER TABLE `production_orders` ADD COLUMN `customerId` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `production_orders` ADD COLUMN `saleId` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `production_orders` ADD COLUMN `actualQuantity` REAL DEFAULT NULL")
        db.execSQL("ALTER TABLE `production_orders` ADD COLUMN `actualMaterialCost` INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE `production_orders` ADD COLUMN `remarks` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `production_orders` ADD COLUMN `materialsDeducted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `production_orders` ADD COLUMN `cancelledAt` TEXT DEFAULT NULL")

        db.execSQL("ALTER TABLE `sales` ADD COLUMN `fulfillmentStatus` TEXT NOT NULL DEFAULT 'DELIVERED'")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE `recipes`
            ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED'
            """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE `recipes`
            ADD COLUMN `updatedAt` TEXT NOT NULL DEFAULT ''
            """.trimIndent()
        )
    }
}

val databaseModule = module {
    single {
        Room.databaseBuilder(
            context = androidContext(),
            name = ZenithDatabase.DATABASE_NAME,
            klass = ZenithDatabase::class.java
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()
    }

    single { get<ZenithDatabase>().productDao }
    single { get<ZenithDatabase>().productVariantDao }
    single { get<ZenithDatabase>().stockVariantDao }
    single { get<ZenithDatabase>().saleDao }
    single { get<ZenithDatabase>().customerDao }
    single { get<ZenithDatabase>().expenseDao }
    single { get<ZenithDatabase>().branchDao }
    single { get<ZenithDatabase>().businessSettingsDao }
    single { get<ZenithDatabase>().materialDao }
    single { get<ZenithDatabase>().productionOrderDao }
    single { get<ZenithDatabase>().categoryDao }
    single { get<ZenithDatabase>().debtPaymentDao }
    single { get<ZenithDatabase>().terminalDao }
    single { get<ZenithDatabase>().paymentDao }
    single { get<ZenithDatabase>().auditLogDao }
}
