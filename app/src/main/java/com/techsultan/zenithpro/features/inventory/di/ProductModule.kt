package com.techsultan.zenithpro.features.inventory.di

import com.techsultan.zenithpro.core.manager.SyncManager
import com.techsultan.zenithpro.features.expenses.data.repository.ExpenseRepositoryImpl
import com.techsultan.zenithpro.features.expenses.domain.repository.ExpenseRepository
import com.techsultan.zenithpro.features.inventory.data.repository.ProductRepositoryImpl
import com.techsultan.zenithpro.features.inventory.domain.repository.ProductRepository
import com.techsultan.zenithpro.features.inventory.domain.use_case.AddProductUseCase
import com.techsultan.zenithpro.features.inventory.domain.use_case.DeleteProductUseCase
import com.techsultan.zenithpro.features.inventory.domain.use_case.GetProductsUseCase
import com.techsultan.zenithpro.features.inventory.domain.use_case.SyncProductsUseCase
import com.techsultan.zenithpro.features.inventory.presentation.AddProductViewModel
import com.techsultan.zenithpro.features.inventory.presentation.InventoryViewModel
import com.techsultan.zenithpro.features.inventory.presentation.PrintBarcodeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val productModule = module {
    single<ProductRepository> { ProductRepositoryImpl(
        imageUploadManager = get(),
        postgrest = get(),
        functions = get(),
        networkMonitor = get(),
        productDao = get(),
        variantDao = get(),
        stockDao = get(),
        imageCacheManager = get()
    ) }

    single {
        SyncManager(
            repository = get<ProductRepository>() as ProductRepositoryImpl,
            expenseRepository = get<ExpenseRepository>() as ExpenseRepositoryImpl,
            expenseDao = get(),
            productDao = get(),
            networkMonitor = get(),
        )
    }

    factory { AddProductUseCase(get()) }
    factory { GetProductsUseCase(get()) }
    factory { SyncProductsUseCase(get(), get(), get()) }
    factory { DeleteProductUseCase(get()) }

    viewModel { AddProductViewModel(get(), get(), get()) }

    viewModel {
        InventoryViewModel(
            getProductsUseCase = get(),
            syncProductsUseCase = get(),
            deleteProductUseCase = get(),
            networkMonitor = get(),
            sessionManager = get()
        )
    }

    viewModelOf(::PrintBarcodeViewModel)

}