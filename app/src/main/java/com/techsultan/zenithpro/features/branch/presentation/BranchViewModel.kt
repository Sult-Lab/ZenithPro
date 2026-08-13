package com.techsultan.zenithpro.features.branch.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
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
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(BranchUiState())
    val state: StateFlow<BranchUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<BranchEvent>()
    val events = _events.asSharedFlow()
    
    var isAdmin: Boolean = false
        private set

    private var businessId: String? = null

    init {
        viewModelScope.launch {
            val session = sessionManager.loadSession()
            businessId = session?.businessId
            isAdmin = session?.isAdmin ?: false
            
            if (businessId != null) {
                observeBranches()
                pullFromServer()
            }
        }
    }

    private fun observeBranches() {
        val bId = businessId ?: return
        viewModelScope.launch {
            getBranchesUseCase(bId).collect { result ->
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
    }

    fun pullFromServer() {
        val bId = businessId ?: return
        viewModelScope.launch { branchRepository.pullFromServer(bId) }
    }

    fun upsertBranch(
        id: String?, name: String, address: String?,
        phone: String?
    ) {
        val bId = businessId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            when (val result = upsertBranchUseCase(id, name, address, phone, bId)) {
                is Resource.Success -> {
                    _state.update { it.copy(isSaving = false) }
                    _events.emit(BranchEvent.Saved)
                    observeBranches()
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

    fun toggleActive(branchId: String, isActive: Boolean) {
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
