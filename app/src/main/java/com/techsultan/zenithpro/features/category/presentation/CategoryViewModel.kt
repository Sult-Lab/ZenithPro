package com.techsultan.zenithpro.features.category.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.category.data.local.CategoryEntity
import com.techsultan.zenithpro.features.category.domain.use_case.DeleteCategoryUseCase
import com.techsultan.zenithpro.features.category.domain.use_case.GetCategoriesUseCase
import com.techsultan.zenithpro.features.category.domain.use_case.PullCategoriesUseCase
import com.techsultan.zenithpro.features.category.domain.use_case.SearchCategoryUseCase
import com.techsultan.zenithpro.features.category.domain.use_case.UpsertCategoryUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val sessionManager: SessionManager,
    private val searchCategoryUseCase: SearchCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val upsertCategoryUseCase: UpsertCategoryUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val pullCategoriesUseCase: PullCategoriesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryUiState())
    val state: StateFlow<CategoryUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CategoryEvent>()
    val events = _events.asSharedFlow()

    init {
        observe()
        viewModelScope.launch {
            pullCategoriesUseCase(sessionManager.businessId)
        }
    }

    private fun observe() {
        viewModelScope.launch {
            getCategoriesUseCase(sessionManager.businessId)
                .collect { result ->
                    when (result) {
                        is Resource.Success -> _state.update {
                            it.copy(isLoading = false, categories = result.data ?: emptyList())
                        }
                        is Resource.Error   -> _state.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                        is Resource.Loading -> _state.update { it.copy(isLoading = true) }
                    }
                }
        }
    }

    fun upsertCategory(id: String?, name: String, color: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val result = upsertCategoryUseCase(
                id, sessionManager.businessId, color, name
            )
            when (result) {
                is Resource.Success -> {
                    _state.update { it.copy(isSaving = false) }
                    _events.emit(CategoryEvent.Saved)
                }
                is Resource.Error -> {
                    _state.update { it.copy(isSaving = false) }
                    _events.emit(CategoryEvent.ShowError(result.message ?: "Failed"))
                }
                else -> _state.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            deleteCategoryUseCase(categoryId)
            _events.emit(CategoryEvent.Deleted)
        }
    }

    sealed class CategoryEvent {
        data object Saved : CategoryEvent()
        data object Deleted : CategoryEvent()
        data class ShowError(val message: String) : CategoryEvent()
    }
}

data class CategoryUiState(
    val isLoading: Boolean              = false,
    val isSaving: Boolean               = false,
    val categories: List<CategoryEntity> = emptyList(),
    val error: String?                  = null
)
