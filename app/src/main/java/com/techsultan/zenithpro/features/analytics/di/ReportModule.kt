package com.techsultan.zenithpro.features.analytics.di

import com.techsultan.zenithpro.features.analytics.data.repository.ReportsRepositoryImpl
import com.techsultan.zenithpro.features.analytics.domain.ReportsRepository
import com.techsultan.zenithpro.features.analytics.presentation.ReportsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val reportsModule = module {
    single<ReportsRepository> {
        ReportsRepositoryImpl(
            saleDao = get(),
            expenseDao = get(),
            customerDao = get(),
            branchDao = get(),
            postgrest = get(),
            saleRepository = get(),
            expenseRepository = get(),
            customerRepository = get(),
            networkMonitor = get()
        )
    }
    viewModel { ReportsViewModel(get(), get(), get()) }
}