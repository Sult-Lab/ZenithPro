package com.techsultan.zenithpro.features.settings.di

import com.techsultan.zenithpro.features.settings.data.repository.PaymentSettingsRepositoryImpl
import com.techsultan.zenithpro.features.settings.domain.repository.PaymentSettingsRepository
import com.techsultan.zenithpro.features.settings.domain.use_case.GetTerminalsUseCase
import com.techsultan.zenithpro.features.settings.domain.use_case.SyncTerminalsUseCase
import com.techsultan.zenithpro.features.settings.domain.use_case.UpdateSweepAccountUseCase
import com.techsultan.zenithpro.features.settings.presentation.viewmodel.PaymentSettingsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val paymentModule = module {

    single<PaymentSettingsRepository> {
        PaymentSettingsRepositoryImpl(get(), get(), get())
    }

    factoryOf(::GetTerminalsUseCase)
    factoryOf(::SyncTerminalsUseCase)
    factoryOf(::UpdateSweepAccountUseCase)

    viewModel { PaymentSettingsViewModel(get(), get(), get(), get()) }
}
