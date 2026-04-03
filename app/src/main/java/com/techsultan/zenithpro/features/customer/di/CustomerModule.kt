package com.techsultan.zenithpro.features.customer.di

import com.techsultan.zenithpro.features.customer.data.repository.CustomerRepositoryImpl
import com.techsultan.zenithpro.features.customer.domain.repository.CustomerRepository
import com.techsultan.zenithpro.features.customer.domain.use_case.GetCustomerDetailUseCase
import com.techsultan.zenithpro.features.customer.domain.use_case.GetCustomerReportsUseCase
import com.techsultan.zenithpro.features.customer.domain.use_case.GetCustomersUseCase
import com.techsultan.zenithpro.features.customer.domain.use_case.RecordDebtPaymentUseCase
import com.techsultan.zenithpro.features.customer.domain.use_case.UpsertCustomerUseCase
import com.techsultan.zenithpro.features.customer.presentation.viewmodel.AddEditCustomerViewModel
import com.techsultan.zenithpro.features.customer.presentation.viewmodel.CustomerDetailViewModel
import com.techsultan.zenithpro.features.customer.presentation.viewmodel.CustomerListViewModel
import com.techsultan.zenithpro.features.customer.presentation.viewmodel.CustomerReportsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val customerModule = module {

    single<CustomerRepository> {
        CustomerRepositoryImpl(
            customerDao = get(),
            saleDao = get(),
            functions = get(),
            postgrest = get(),
            networkMonitor = get()
        )
    }

    factory { UpsertCustomerUseCase(get()) }
    factory { GetCustomersUseCase(get()) }
    factory { GetCustomerDetailUseCase(get()) }
    factory { GetCustomerReportsUseCase(get()) }
    factory { RecordDebtPaymentUseCase(get()) }

    viewModel { CustomerListViewModel(get(), get(), get()) }
    viewModel { AddEditCustomerViewModel(get()) }
    viewModel { CustomerDetailViewModel(get(), get()) }
    viewModel { CustomerReportsViewModel(get(), get(), get()) }
}