package com.techsultan.zenithpro.features.expenses.di

import com.techsultan.zenithpro.core.database.ZenithDatabase
import com.techsultan.zenithpro.features.expenses.data.repository.ExpenseRepositoryImpl
import com.techsultan.zenithpro.features.expenses.domain.repository.ExpenseRepository
import com.techsultan.zenithpro.features.expenses.domain.use_case.DeleteExpenseUseCase
import com.techsultan.zenithpro.features.expenses.domain.use_case.GetExpenseStatsUseCase
import com.techsultan.zenithpro.features.expenses.domain.use_case.GetExpensesUseCase
import com.techsultan.zenithpro.features.expenses.domain.use_case.UpsertExpenseUseCase
import com.techsultan.zenithpro.features.expenses.viewmodel.AddEditExpenseViewModel
import com.techsultan.zenithpro.features.expenses.viewmodel.ExpenseListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val expenseModule = module {

    single<ExpenseRepository> {
        ExpenseRepositoryImpl(
            expenseDao = get(),
            postgrest = get(),
            networkMonitor = get()
        )
    }

    factory { UpsertExpenseUseCase(get()) }
    factory { GetExpensesUseCase(get()) }
    factory { GetExpenseStatsUseCase(get()) }
    factory { DeleteExpenseUseCase(get()) }

    viewModel { ExpenseListViewModel(get(), get(), get(), get(), get()) }
    viewModel { AddEditExpenseViewModel(get(), get()) }
}