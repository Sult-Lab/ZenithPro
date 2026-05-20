package com.techsultan.zenithpro.features.settings.di

import com.techsultan.zenithpro.features.settings.data.repository.BusinessRepositoryImpl
import com.techsultan.zenithpro.features.settings.domain.repository.BusinessRepository
import com.techsultan.zenithpro.features.settings.domain.use_case.UpdateBusinessUseCase
import com.techsultan.zenithpro.features.settings.presentation.EditBusinessViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val businessModule = module {
    single<BusinessRepository> {
        BusinessRepositoryImpl(
            functions = get(),
            imageUploadManager = get(),
            sessionManager = get(),
            storage = get(),
            context = androidContext()
        )
    }
    factory { UpdateBusinessUseCase(get()) }
    viewModel { EditBusinessViewModel(get(), get(), get()) }
}