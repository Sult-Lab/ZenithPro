package com.techsultan.zenithpro.features.auth.di

import com.techsultan.zenithpro.features.auth.data.repository.AuthenticationRepositoryImpl
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import com.techsultan.zenithpro.features.settings.domain.use_case.CreateStaffUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.IsUserLoggedInUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.LoginUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.LogoutUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.SignUpUseCase
import com.techsultan.zenithpro.features.auth.presentation.AuthViewModel
import com.techsultan.zenithpro.features.auth.presentation.ChangePasswordViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    single<AuthenticationRepository> {
        AuthenticationRepositoryImpl(get(), get(), get(), get(), get(), get())
    }

    factory { SignUpUseCase(get()) }
    factory { LoginUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { IsUserLoggedInUseCase(get()) }
    factory { CreateStaffUseCase(get()) }

    viewModel {
        AuthViewModel(get(), get(), get(), get(), get())
    }

    viewModel {
        ChangePasswordViewModel(get(), get())
    }
}
