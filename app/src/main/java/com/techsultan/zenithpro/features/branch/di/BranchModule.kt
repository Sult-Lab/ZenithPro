package com.techsultan.zenithpro.features.branch.di

import com.techsultan.zenithpro.features.branch.data.repository.BranchRepositoryImpl
import com.techsultan.zenithpro.features.branch.domain.repository.BranchRepository
import com.techsultan.zenithpro.features.branch.domain.use_case.DeleteBranchUseCase
import com.techsultan.zenithpro.features.branch.domain.use_case.GetBranchesUseCase
import com.techsultan.zenithpro.features.branch.domain.use_case.UpsertBranchUseCase
import com.techsultan.zenithpro.features.branch.presentation.BranchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val branchModule = module {

    single<BranchRepository> {
        BranchRepositoryImpl(get(), get(), get())
    }
    factory { GetBranchesUseCase(get()) }
    factory { UpsertBranchUseCase(get()) }
    factory { DeleteBranchUseCase(get()) }
    viewModel { BranchViewModel(get(), get(), get(), get(), get()) }
}
