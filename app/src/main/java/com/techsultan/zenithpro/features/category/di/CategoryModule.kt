package com.techsultan.zenithpro.features.category.di

import com.techsultan.zenithpro.core.database.ZenithDatabase
import com.techsultan.zenithpro.features.category.data.repository.CategoryRepositoryImpl
import com.techsultan.zenithpro.features.category.domain.repository.CategoryRepository
import com.techsultan.zenithpro.features.category.domain.use_case.DeleteCategoryUseCase
import com.techsultan.zenithpro.features.category.domain.use_case.GetCategoriesUseCase
import com.techsultan.zenithpro.features.category.domain.use_case.PullCategoriesUseCase
import com.techsultan.zenithpro.features.category.domain.use_case.SearchCategoryUseCase
import com.techsultan.zenithpro.features.category.domain.use_case.UpsertCategoryUseCase
import com.techsultan.zenithpro.features.category.presentation.CategoryViewModel
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel

val categoryModule = module {

    single<CategoryRepository> {
        CategoryRepositoryImpl(get(), get(), get())
    }
    factory { GetCategoriesUseCase(get()) }
    factory { UpsertCategoryUseCase(get()) }
    factory { DeleteCategoryUseCase(get()) }
    factory { PullCategoriesUseCase(get()) }
    factory { SearchCategoryUseCase(get()) }

    viewModel { CategoryViewModel(
        sessionManager = get(),
        searchCategoryUseCase = get(),
        deleteCategoryUseCase = get(),
        upsertCategoryUseCase = get(),
        getCategoriesUseCase = get(),
        pullCategoriesUseCase = get()
    ) }
}