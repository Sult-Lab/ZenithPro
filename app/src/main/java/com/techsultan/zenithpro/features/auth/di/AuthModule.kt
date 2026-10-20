package com.techsultan.zenithpro.features.auth.di

import com.techsultan.zenithpro.features.auth.data.repository.AuthenticationRepositoryImpl
import com.techsultan.zenithpro.features.auth.domain.repository.AuthenticationRepository
import com.techsultan.zenithpro.features.settings.domain.use_case.CreateStaffUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.IsUserLoggedInUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.LoginUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.LogoutUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.ResendConfirmationEmailUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.SignUpUseCase
import com.techsultan.zenithpro.features.auth.domain.use_case.VerifySignupOtpUseCase
import com.techsultan.zenithpro.features.auth.presentation.AuthViewModel
import com.techsultan.zenithpro.features.auth.presentation.ChangePasswordViewModel
import com.techsultan.zenithpro.features.auth.presentation.EmailConfirmedViewModel
import com.techsultan.zenithpro.features.auth.presentation.RegistrationPendingViewModel
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
    factory { VerifySignupOtpUseCase(authRepository = get()) }
    factory { ResendConfirmationEmailUseCase(authRepository = get()) }

    viewModel {
        AuthViewModel(
            signUpUseCase = get(),
            loginUseCase = get(),
            logoutUseCase = get(),
            isUserLoggedInUseCase = get(),
            createStaffUseCase = get(),
            analytics = get(),
            verifySignupOtpUseCase = get(),
            resendConfirmationEmailUseCase = get()
        )
    }

    viewModel {
        ChangePasswordViewModel(get(), get(), get(), get())
    }

    viewModel {
        EmailConfirmedViewModel(get(), get())
    }

    viewModel {
        RegistrationPendingViewModel(get())
    }
}
