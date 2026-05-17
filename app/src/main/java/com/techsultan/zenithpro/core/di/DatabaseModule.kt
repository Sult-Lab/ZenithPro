package com.techsultan.zenithpro.core.di

import androidx.room.Room
import com.techsultan.zenithpro.core.database.ZenithDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            context = androidContext(),
            name = ZenithDatabase.DATABASE_NAME,
            klass = ZenithDatabase::class.java
        )
            .fallbackToDestructiveMigration(true)
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
}
