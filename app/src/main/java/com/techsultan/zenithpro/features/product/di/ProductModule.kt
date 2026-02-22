package com.techsultan.zenithpro.features.product.di

import com.techsultan.zenithpro.features.product.data.repository.ProductRepositoryImpl
import com.techsultan.zenithpro.features.product.domain.repository.ProductRepository
import com.techsultan.zenithpro.features.product.domain.use_case.AddProductUseCase
import com.techsultan.zenithpro.features.product.presentation.AddProductViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val productModule = module {
    single<ProductRepository> { ProductRepositoryImpl(get(), get(), get()) }

    factory { AddProductUseCase(get()) }

    viewModel { AddProductViewModel(get()) }

}