package com.techsultan.zenithpro.features.settings.di

import com.techsultan.zenithpro.features.settings.data.repository.PaymentSettingsRepositoryImpl
import com.techsultan.zenithpro.features.settings.domain.repository.PaymentSettingsRepository
import com.techsultan.zenithpro.features.settings.presentation.viewmodel.PaymentSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val paymentModule = module {

    single<PaymentSettingsRepository> {
        PaymentSettingsRepositoryImpl(get(), get(), get())
    }

    viewModel { PaymentSettingsViewModel(get(), get()) }
}