package com.techsultan.zenithpro.core.di

import com.techsultan.zenithpro.core.data.local.PrinterDataStore
import com.techsultan.zenithpro.core.data.repository.PrinterRepositoryImpl
import com.techsultan.zenithpro.core.domain.repository.PrinterRepository
import com.techsultan.zenithpro.core.manager.AppDataStore
import com.techsultan.zenithpro.core.manager.PrinterDriverFactory
import com.techsultan.zenithpro.core.manager.PrinterManager
import com.techsultan.zenithpro.core.manager.ReceiptNumberGenerator
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.BarcodeLabelFormatter
import com.techsultan.zenithpro.core.util.BusinessLogoManager
import com.techsultan.zenithpro.core.util.FileShareManager
import com.techsultan.zenithpro.core.util.ImageCacheManager
import com.techsultan.zenithpro.core.util.ImageUploadManager
import com.techsultan.zenithpro.core.util.ReceiptFormatter
import com.techsultan.zenithpro.core.viewmodel.DataPersistentViewModel
import coil.ImageLoader
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {
    single { ImageUploadManager(get(), androidContext()) }
    single { NetworkMonitor(androidContext()) }
    single { ImageCacheManager(androidContext()) }
    single { FileShareManager(androidContext()) }
    viewModel { DataPersistentViewModel(get(), get(), get(), get(), get()) }
    single { BarcodeLabelFormatter() }
    single { ImageLoader(androidContext()) }
    single { BusinessLogoManager(androidContext(), get()) }

    // Manager/DataStore
    single { AppDataStore(androidContext()) }

    // Printer related
    single { PrinterDataStore(androidContext()) }
    single { PrinterDriverFactory(androidContext()) }
    single { PrinterManager(get()) }
    single<PrinterRepository> { PrinterRepositoryImpl(get(), get()) }
}
