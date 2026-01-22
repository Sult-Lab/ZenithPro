package com.techsultan.zenithpro.core.di

import com.techsultan.zenithpro.core.viewmodel.DataPersistentViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val commonModule = module {
    viewModel { DataPersistentViewModel(get())  }
}