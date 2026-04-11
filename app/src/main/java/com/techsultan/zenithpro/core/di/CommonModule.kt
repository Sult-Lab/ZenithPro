package com.techsultan.zenithpro.core.di

import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.ImageCacheManager
import com.techsultan.zenithpro.core.util.ImageUploadManager
import com.techsultan.zenithpro.core.viewmodel.DataPersistentViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {
    single { ImageUploadManager(get(), androidContext()) }
    single { NetworkMonitor(androidContext()) }
    single { ImageCacheManager(androidContext()) }
    viewModel { DataPersistentViewModel(get(), get(), get(), get()) }
}
