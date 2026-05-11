package com.techsultan.zenithpro.features.sales.di

import com.techsultan.zenithpro.features.sales.data.repository.PrinterRepositoryImpl
import com.techsultan.zenithpro.features.sales.data.repository.SaleRepositoryImpl
import com.techsultan.zenithpro.features.sales.domain.repository.PrinterRepository
import com.techsultan.zenithpro.features.sales.domain.repository.SaleRepository
import com.techsultan.zenithpro.features.sales.domain.use_case.GenerateReceiptUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.GetDailySummaryUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.GetSalesUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.ProcessSaleUseCase
import com.techsultan.zenithpro.features.sales.presentation.CheckoutViewModel
import com.techsultan.zenithpro.features.sales.presentation.SalesListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val salesModule = module {

    single<SaleRepository> {
        SaleRepositoryImpl(
            saleDao = get(),
            functions = get(),
            postgrest = get(),
            networkMonitor = get(),
            sessionManager = get()
        )
    }

    single<PrinterRepository> {
        PrinterRepositoryImpl(
            formatter = get(),
            printerManager = get()
        )
    }

    viewModel { CheckoutViewModel(
        getProductsUseCase = get(),
        processSaleUseCase = get(),
        getSaleUseCase = get(),
        getDailySummaryUseCase = get(),
        sessionManager = get(),
        customerRepository = get(),
        getCustomerDetailUseCase = get()
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
    factory { GenerateReceiptUseCase() }

}
