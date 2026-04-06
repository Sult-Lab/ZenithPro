package com.techsultan.zenithpro.features.sales.di

import com.techsultan.zenithpro.features.sales.data.repository.SaleRepositoryImpl
import com.techsultan.zenithpro.features.sales.domain.repository.SaleRepository
import com.techsultan.zenithpro.features.sales.domain.use_case.GetDailySummaryUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.GetSalesUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.ProcessSaleUseCase
import com.techsultan.zenithpro.features.sales.presentation.NewSaleViewModel
import com.techsultan.zenithpro.features.sales.presentation.SalesListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val salesModule = module {

    single<SaleRepository> {
        SaleRepositoryImpl(
            saleDao = get(),
            functions = get(),
            postgrest = get(),
            networkMonitor = get()
        )
    }

    viewModel { NewSaleViewModel(
        getProductsUseCase = get(),
        processSaleUseCase = get(),
        getSaleUseCase = get(),
        getDailySummaryUseCase = get(),
        sessionManager = get()
    ) }

    viewModel {
        SalesListViewModel(
            getSalesUseCase = get(),
            saleRepository = get(),
            networkMonitor = get(),
            sessionManager = get()
        )
    }

    factory { ProcessSaleUseCase(get()) }
    factory { GetSalesUseCase(get()) }
    factory { GetDailySummaryUseCase(get()) }

}
