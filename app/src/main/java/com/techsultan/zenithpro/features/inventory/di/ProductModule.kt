package com.techsultan.zenithpro.features.inventory.di

import com.techsultan.zenithpro.core.manager.SyncManager
import com.techsultan.zenithpro.core.worker.SyncWorker
import com.techsultan.zenithpro.features.branch.data.repository.BranchRepositoryImpl
import com.techsultan.zenithpro.features.branch.domain.repository.BranchRepository
import com.techsultan.zenithpro.features.category.data.repository.CategoryRepositoryImpl
import com.techsultan.zenithpro.features.category.domain.repository.CategoryRepository
import com.techsultan.zenithpro.features.customer.data.repository.CustomerRepositoryImpl
import com.techsultan.zenithpro.features.customer.domain.repository.CustomerRepository
import com.techsultan.zenithpro.features.expenses.data.repository.ExpenseRepositoryImpl
import com.techsultan.zenithpro.features.expenses.domain.repository.ExpenseRepository
import com.techsultan.zenithpro.features.inventory.data.repository.ProductRepositoryImpl
import com.techsultan.zenithpro.features.inventory.domain.repository.ProductRepository
import com.techsultan.zenithpro.features.inventory.domain.use_case.AddProductUseCase
import com.techsultan.zenithpro.features.inventory.domain.use_case.DeleteProductUseCase
import com.techsultan.zenithpro.features.inventory.domain.use_case.GetProductUseCase
import com.techsultan.zenithpro.features.inventory.domain.use_case.GetProductsUseCase
import com.techsultan.zenithpro.features.inventory.domain.use_case.SyncProductsUseCase
import com.techsultan.zenithpro.features.inventory.domain.use_case.UpdateProductUseCase
import com.techsultan.zenithpro.features.inventory.presentation.AddProductViewModel
import com.techsultan.zenithpro.features.inventory.presentation.InventoryViewModel
import com.techsultan.zenithpro.features.inventory.presentation.PrintBarcodeViewModel
import com.techsultan.zenithpro.features.inventory.presentation.ProductDetailViewModel
import com.techsultan.zenithpro.features.production.data.repository.ProductionRepositoryImpl
import com.techsultan.zenithpro.features.production.domain.repository.ProductionRepository
import com.techsultan.zenithpro.features.sales.data.repository.SaleRepositoryImpl
import com.techsultan.zenithpro.features.sales.domain.repository.SaleRepository
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
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

    worker {
        SyncWorker(
            get(),
            get(),
            productRepository = get<ProductRepository>() as ProductRepositoryImpl,
            expenseRepository = get<ExpenseRepository>() as ExpenseRepositoryImpl,
            categoryRepository = get<CategoryRepository>() as CategoryRepositoryImpl,
            branchRepository = get<BranchRepository>() as BranchRepositoryImpl,
            customerRepository = get<CustomerRepository>() as CustomerRepositoryImpl,
            saleRepository = get<SaleRepository>() as SaleRepositoryImpl,
            productionRepository = get<ProductionRepository>() as ProductionRepositoryImpl,
            productDao = get(),
            expenseDao = get(),
            categoryDao = get(),
            branchDao = get(),
            customerDao = get(),
            saleDao = get(),
            productionDao = get(),
            networkMonitor = get(),
        )
    }

    single {
        SyncManager(androidContext())
    }

    factory { AddProductUseCase(get(), get()) }
    factory { GetProductsUseCase(get()) }
    factory { GetProductUseCase(get()) }
    factory { UpdateProductUseCase(get()) }
    factory { SyncProductsUseCase(get(), get(), get()) }
    factory { DeleteProductUseCase(get()) }

    viewModel { AddProductViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ProductDetailViewModel(get(), get(), get(), get(), get(), get()) }

    viewModel {
        InventoryViewModel(
            getProductsUseCase = get(),
            syncProductsUseCase = get(),
            deleteProductUseCase = get(),
            networkMonitor = get(),
            sessionManager = get(),
            branchDao = get()
        )
    }

    viewModelOf(::PrintBarcodeViewModel)

}
