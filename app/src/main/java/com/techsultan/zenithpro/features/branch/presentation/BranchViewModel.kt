package com.techsultan.zenithpro.features.branch.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.branch.domain.repository.BranchRepository
import com.techsultan.zenithpro.features.branch.domain.use_case.DeleteBranchUseCase
import com.techsultan.zenithpro.features.branch.domain.use_case.GetBranchesUseCase
import com.techsultan.zenithpro.features.branch.domain.use_case.UpsertBranchUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BranchViewModel(
    private val getBranchesUseCase: GetBranchesUseCase,
    private val upsertBranchUseCase: UpsertBranchUseCase,
    private val deleteBranchUseCase: DeleteBranchUseCase,
    private val branchRepository: BranchRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BranchUiState())
    val state: StateFlow<BranchUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<BranchEvent>()
    val events = _events.asSharedFlow()

    fun init(businessId: String) {
        viewModelScope.launch {
            getBranchesUseCase(businessId).collect { result ->
                when (result) {
                    is Resource.Success -> _state.update {
                        it.copy(isLoading = false, branches = result.data ?: emptyList())
                    }
                    is Resource.Error   -> _state.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                    is Resource.Loading -> _state.update { it.copy(isLoading = true) }
                }
            }
        }
        viewModelScope.launch { branchRepository.pullFromServer(businessId) }
    }

    fun upsertBranch(
        id: String?, name: String, address: String?,
        phone: String?, businessId: String
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            when (val result = upsertBranchUseCase(id, name, address, phone, businessId)) {
                is Resource.Success -> {
                    _state.update { it.copy(isSaving = false) }
                    _events.emit(BranchEvent.Saved)
                }
                is Resource.Error -> {
                    _state.update { it.copy(isSaving = false) }
                    _events.emit(BranchEvent.ShowError(result.message ?: "Failed"))
                }
                else -> Unit
            }
        }
    }

    fun deleteBranch(branchId: String) {
        viewModelScope.launch {
            deleteBranchUseCase(branchId)
            _events.emit(BranchEvent.Deleted)
        }
    }

    fun toggleActive(branchId: String, isActive: Boolean, businessId: String) {
        viewModelScope.launch {
            branchRepository.toggleBranchActive(branchId, isActive)
        }
    }

    sealed class BranchEvent {
        data object Saved : BranchEvent()
        data object Deleted : BranchEvent()
        data class ShowError(val message: String) : BranchEvent()
    }
}

data class BranchUiState(
    val isLoading: Boolean            = false,
    val isSaving: Boolean             = false,
    val branches: List<BranchEntity>  = emptyList(),
    val error: String?                = null
)