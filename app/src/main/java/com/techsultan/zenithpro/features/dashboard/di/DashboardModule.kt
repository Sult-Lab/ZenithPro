package com.techsultan.zenithpro.features.dashboard.di

import com.techsultan.zenithpro.features.dashboard.data.repository.DashboardRepositoryImpl
import com.techsultan.zenithpro.features.dashboard.domain.repository.DashboardRepository
import com.techsultan.zenithpro.features.dashboard.domain.use_case.GetChartDataUseCase
import com.techsultan.zenithpro.features.dashboard.domain.use_case.GetDashboardSummaryUseCase
import com.techsultan.zenithpro.features.dashboard.domain.use_case.GetPendingDebtsUseCase
import com.techsultan.zenithpro.features.dashboard.domain.use_case.GetUrgentActionUseCase
import com.techsultan.zenithpro.features.dashboard.domain.use_case.SyncDashboardUseCase
import com.techsultan.zenithpro.features.dashboard.presentation.DashboardViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dashboardModule = module {
    single<DashboardRepository> {
        DashboardRepositoryImpl(
            saleDao = get(),
            saleRepository = get(),
            productRepository = get()
        )
    }

    factory { GetDashboardSummaryUseCase(get()) }
    factory { GetChartDataUseCase(get()) }
    factory { GetPendingDebtsUseCase(get()) }
    factory { SyncDashboardUseCase(get(), get()) }
    factory { GetUrgentActionUseCase(get()) }

    viewModel {
        DashboardViewModel(
            getDashboardSummaryUseCase = get(),
            getChartDataUseCase = get(),
            getPendingDebtsUseCase = get(),
            syncDashboardUseCase = get(),
            getUrgentActionUseCase = get(),
            networkMonitor = get(),
            sessionManager = get(),
            branchDao = get(),
            pullCategoriesUseCase = get()
        )
    }
}
