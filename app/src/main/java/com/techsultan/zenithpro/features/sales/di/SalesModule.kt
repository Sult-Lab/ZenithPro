package com.techsultan.zenithpro.features.sales.di

import com.techsultan.zenithpro.core.worker.SalePaymentPollerWorker
import com.techsultan.zenithpro.features.sales.data.repository.SaleRepositoryImpl
import com.techsultan.zenithpro.features.sales.domain.repository.SaleRepository
import com.techsultan.zenithpro.features.sales.domain.use_case.GenerateReceiptUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.GetDailySummaryUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.GetSalesUseCase
import com.techsultan.zenithpro.features.sales.domain.use_case.ProcessSaleUseCase
import com.techsultan.zenithpro.features.sales.presentation.CheckoutViewModel
import com.techsultan.zenithpro.features.sales.presentation.ReceiptViewModel
import com.techsultan.zenithpro.features.sales.presentation.SaleDetailViewModel
import com.techsultan.zenithpro.features.sales.presentation.SalesListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val salesModule = module {

    worker { SalePaymentPollerWorker(get(), get(), get()) }

    single<SaleRepository> {
        SaleRepositoryImpl(
            context = androidContext(),
            saleDao = get(),
            functions = get(),
            postgrest = get(),
            networkMonitor = get(),
            sessionManager = get(),
            debtPaymentDao = get(),
            branchDao = get()
        )
    }

    viewModel { CheckoutViewModel(
        getProductsUseCase = get(),
        processSaleUseCase = get(),
        sessionManager = get(),
        customerRepository = get(),
        getCustomerDetailUseCase = get(),
        generateReceiptUseCase = get(),
        receiptNumberGenerator = get(),
        getSettingsUseCase = get(),
        getBranchesUseCase = get(),
        getTerminalsUseCase = get(),
    ) }

    viewModel {
        SalesListViewModel(
            getSalesUseCase = get(),
            saleRepository = get(),
            networkMonitor = get(),
            sessionManager = get(),
            generateReceiptUseCase = get(),
            receiptNumberGenerator = get(),
            getSettingsUseCase = get(),
            getBranchesUseCase = get(),
            getStaffListUseCase = get(),
        )
    }

    viewModel {
        ReceiptViewModel(
            printerRepository = get(),
            printerDataStore = get(),
            receiptPdfGenerator = get(),
            receiptImageGenerator = get(),
            fileShareManager = get(),
            logoManager = get()
        )
    }

    viewModel {
        SaleDetailViewModel(
            saleDao = get(),
            sessionManager = get()
        )
    }

    factory { ProcessSaleUseCase(get(), get()) }
    factory { GetSalesUseCase(get()) }
    factory { GetDailySummaryUseCase(get()) }
    factory { GenerateReceiptUseCase() }

}
