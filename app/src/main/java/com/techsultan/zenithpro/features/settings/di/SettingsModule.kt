package com.techsultan.zenithpro.features.settings.di

import com.techsultan.zenithpro.features.settings.data.repository.SettingsRepositoryImpl
import com.techsultan.zenithpro.features.settings.domain.repository.SettingsRepository
import com.techsultan.zenithpro.features.settings.domain.use_case.GetSettingsUseCase
import com.techsultan.zenithpro.features.settings.domain.use_case.GetStaffListUseCase
import com.techsultan.zenithpro.features.settings.domain.use_case.UpdateSettingsUseCase
import com.techsultan.zenithpro.features.settings.domain.use_case.UpdateStaffRoleUseCase
import com.techsultan.zenithpro.features.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {

    single<SettingsRepository> {
        SettingsRepositoryImpl(get(), get(), get())
    }
    factory { GetSettingsUseCase(get()) }
    factory { UpdateSettingsUseCase(get()) }
    factory { GetStaffListUseCase(get()) }
    factory { UpdateStaffRoleUseCase(get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get()) }
}