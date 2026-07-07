package com.techsultan.zenithpro.features.payment.di

import com.techsultan.zenithpro.features.payment.data.repository.PaymentRepository
import com.techsultan.zenithpro.features.payment.domain.repository.PaymentGateway
import com.techsultan.zenithpro.features.payment.presentation.TransferPaymentViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val paymentProcessingModule = module {

    single { PaymentRepository(get(), get(), get()) }
    
    // If you prefer to use the interface PaymentGateway in your code:
    // single<PaymentGateway> { get<PaymentRepository>() }

    viewModel { TransferPaymentViewModel(get()) }
}
