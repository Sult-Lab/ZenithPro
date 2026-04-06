package com.techsultan.zenithpro.features.material.di

import com.techsultan.zenithpro.features.material.data.repository.MaterialRepositoryImpl
import com.techsultan.zenithpro.features.material.domain.repository.MaterialRepository
import com.techsultan.zenithpro.features.material.domain.use_case.AdjustMaterialStockUseCase
import com.techsultan.zenithpro.features.material.domain.use_case.GetMaterialsUseCase
import com.techsultan.zenithpro.features.material.domain.use_case.SaveRecipeUseCase
import com.techsultan.zenithpro.features.material.domain.use_case.UpsertMaterialUseCase
import com.techsultan.zenithpro.features.material.presentation.MaterialViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val materialModule = module {

    single<MaterialRepository> {
        MaterialRepositoryImpl(get(), get(), get())
    }

    factory { GetMaterialsUseCase(get()) }
    factory { UpsertMaterialUseCase(get()) }
    factory { AdjustMaterialStockUseCase(get()) }
    factory { SaveRecipeUseCase(get()) }

    viewModel { MaterialViewModel(
        get(),
        get(),
        get(),
        get(),
        get(),
        get()
    ) }

}