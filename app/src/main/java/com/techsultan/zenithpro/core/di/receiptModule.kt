package com.techsultan.zenithpro.core.di

import com.techsultan.zenithpro.core.manager.ReceiptNumberGenerator
import com.techsultan.zenithpro.core.util.ReceiptFormatter
import com.techsultan.zenithpro.core.util.ReceiptImageGenerator
import com.techsultan.zenithpro.core.util.ReceiptPdfGenerator
import com.techsultan.zenithpro.core.util.ReceiptRenderer
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val receiptModule = module {

    single { ReceiptFormatter() }
    single { ReceiptNumberGenerator(get()) }
    single {
        ReceiptRenderer(
            logoManager = get()
        )
    }

    single {
        ReceiptPdfGenerator(
            context = androidContext(),
            renderer = get()
        )
    }

    single {
        ReceiptImageGenerator(
            context = androidContext(),
            renderer = get()
        )
    }
}
