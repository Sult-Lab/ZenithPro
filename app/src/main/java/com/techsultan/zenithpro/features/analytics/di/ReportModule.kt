package com.techsultan.zenithpro.features.analytics.di

import com.techsultan.zenithpro.features.analytics.presentation.ReportsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val reportsModule = module {
    viewModel {
        ReportsViewModel(get(), get(), get(), get(), get(), get())
    }
}