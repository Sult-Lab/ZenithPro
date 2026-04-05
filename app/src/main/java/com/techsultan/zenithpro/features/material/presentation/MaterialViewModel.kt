package com.techsultan.zenithpro.features.material.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.network.NetworkMonitor
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.material.data.local.MaterialEntity
import com.techsultan.zenithpro.features.material.data.remote.UpsertMaterialRequest
import com.techsultan.zenithpro.features.material.domain.repository.MaterialRepository
import com.techsultan.zenithpro.features.material.domain.use_case.AdjustMaterialStockUseCase
import com.techsultan.zenithpro.features.material.domain.use_case.GetMaterialsUseCase
import com.techsultan.zenithpro.features.material.domain.use_case.UpsertMaterialUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MaterialViewModel(
    private val getMaterialsUseCase: GetMaterialsUseCase,
    private val upsertMaterialUseCase: UpsertMaterialUseCase,
    private val adjustStockUseCase: AdjustMaterialStockUseCase,
    private val materialRepository: MaterialRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(MaterialUiState())
    val state: StateFlow<MaterialUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<MaterialEvent>()
    val events = _events.asSharedFlow()

    private var businessId: String? = null

    init {
        viewModelScope.launch {
            businessId = sessionManager.loadSession()?.businessId
            if (businessId != null){
                init(businessId)
            }
        }
    }

    fun init(businessId: String?) {
        viewModelScope.launch {
            val bId = businessId ?: return@launch
            getMaterialsUseCase(bId).collect { result ->
                when (result) {
                    is Resource.Success -> _state.update {
                        it.copy(isLoading = false, materials = result.data ?: emptyList())
                    }
                    is Resource.Error -> _state.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                    is Resource.Loading -> _state.update { it.copy(isLoading = true) }
                }
            }
        }
        viewModelScope.launch {
            val bId = businessId ?: return@launch
            materialRepository.getLowStockMaterials(bId).collect { result ->
                if (result is Resource.Success) {
                    _state.update { it.copy(lowStockMaterials = result.data ?: emptyList()) }
                }
            }
        }
        viewModelScope.launch {
            val bId = businessId ?: return@launch
            if (networkMonitor.isConnected()) materialRepository.pullFromServer(bId)
        }
    }

    fun onStatusFilterChanged(status: String?) {
        _state.update { it.copy(selectedStatus = status) }
        viewModelScope.launch {
            val bId = businessId ?: return@launch
            getMaterialsUseCase(bId, status).collect { result ->
                if (result is Resource.Success) {
                    _state.update { it.copy(materials = result.data ?: emptyList() ) }
                }
            }
        }
    }

    fun upsertMaterial(request: UpsertMaterialRequest) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            when (val result = upsertMaterialUseCase(request, sessionManager.businessId)) {
                is Resource.Success -> {
                    _state.update { it.copy(isSaving = false) }
                    _events.emit(MaterialEvent.Saved)
                }
                is Resource.Error -> {
                    _state.update { it.copy(isSaving = false) }
                    _events.emit(MaterialEvent.ShowError(result.message ?: "Failed"))
                }
                else -> Unit
            }
        }
    }

    fun adjustStock(
        materialId: String, quantity: Double,
        notes: String?
    ) {
        viewModelScope.launch {
            when (val result = adjustStockUseCase(materialId, quantity, notes, sessionManager.userId, sessionManager.businessId)) {
                is Resource.Success -> _events.emit(MaterialEvent.StockAdjusted)
                is Resource.Error   -> _events.emit(MaterialEvent.ShowError(result.message ?: "Failed"))
                else -> Unit
            }
        }
    }

    fun updateStatus(materialId: String, status: String) {
        viewModelScope.launch {
            materialRepository.updateStatus(materialId, status)
        }
    }

    sealed class MaterialEvent {
        data object Saved : MaterialEvent()
        data object StockAdjusted : MaterialEvent()
        data class ShowError(val message: String) : MaterialEvent()
    }
}

data class MaterialUiState(
    val isLoading: Boolean              = false,
    val isSaving: Boolean               = false,
    val materials: List<MaterialEntity> = emptyList(),
    val lowStockMaterials: List<MaterialEntity> = emptyList(),
    val selectedStatus: String?         = null,
    val error: String?                  = null
)