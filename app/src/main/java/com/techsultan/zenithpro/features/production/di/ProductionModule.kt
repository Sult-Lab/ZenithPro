package com.techsultan.zenithpro.features.production.di

import com.techsultan.zenithpro.features.production.data.repository.ProductionRepositoryImpl
import com.techsultan.zenithpro.features.production.domain.repository.ProductionRepository
import com.techsultan.zenithpro.features.production.domain.use_case.CompleteProductionOrderUseCase
import com.techsultan.zenithpro.features.production.domain.use_case.CreateProductionOrderUseCase
import com.techsultan.zenithpro.features.production.domain.use_case.GetProductionOrdersUseCase
import com.techsultan.zenithpro.features.production.presentation.ProductionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val productionModule = module {

    single<ProductionRepository> {
        ProductionRepositoryImpl(get(), get(), get(), get())
    }

    factory { GetProductionOrdersUseCase(get()) }
    factory { CreateProductionOrderUseCase(get()) }
    factory { CompleteProductionOrderUseCase(get()) }

    viewModel { ProductionViewModel(
        get(),
        get(),
        get(),
        get(),
        get(),
        get(),
        get()
    ) }
}