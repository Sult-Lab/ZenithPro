package com.techsultan.zenithpro.features.inventory.presentation

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.ImageCacheManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.branch.data.local.BranchDao
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.category.data.local.CategoryEntity
import com.techsultan.zenithpro.features.category.domain.use_case.GetCategoriesUseCase
import com.techsultan.zenithpro.features.category.domain.use_case.UpsertCategoryUseCase
import com.techsultan.zenithpro.features.inventory.data.remote.AddProductRequest
import com.techsultan.zenithpro.features.inventory.data.remote.ProductVariantCreateRequest
import com.techsultan.zenithpro.features.inventory.domain.use_case.AddProductUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class AddProductViewModel(
    private val addProductUseCase: AddProductUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    val upsertCategoryUseCase: UpsertCategoryUseCase,
    private val imageCacheManager: ImageCacheManager,
    private val sessionManager: SessionManager,
    private val branchDao: BranchDao
) : ViewModel() {

    private val _state = mutableStateOf(ProductUiState())
    val state: State<ProductUiState> = _state

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _branches = MutableStateFlow<List<BranchEntity>>(emptyList())
    val branches: StateFlow<List<BranchEntity>> = _branches.asStateFlow()

    private val _selectedBranchId = MutableStateFlow<String?>(null)
    val selectedBranchId: StateFlow<String?> = _selectedBranchId.asStateFlow()

    val isAdmin: Boolean get() = sessionManager.currentSession?.isAdmin ?: false

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _scannedBarcode = mutableStateOf<String?>(null)
    val scannedBarcode: State<String?> = _scannedBarcode

    val businessId: String? get() = try { sessionManager.businessId } catch (e: Exception) { null }
    val branchId: String? get() = try { sessionManager.currentSession?.branchId } catch (e: Exception) { null }

    fun onBarcodeScanned(barcode: String) {
        _scannedBarcode.value = barcode
    }

    fun onBranchSelected(branchId: String) {
        _selectedBranchId.value = branchId
    }

    fun clearScannedBarcode() {
        _scannedBarcode.value = null
    }

    init {
        viewModelScope.launch {
            sessionManager.loadSession()
            observeCategories()
            loadBranches()
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            val bId = businessId ?: return@launch
            getCategoriesUseCase(bId).collect { result ->
                if (result is Resource.Success) {
                    _categories.value = result.data ?: emptyList()
                }
            }
        }
    }

    fun addProduct(
        addProductRequest: AddProductRequest,
        imageUris: List<Uri>,
        barcode: String? = null
    ) {
        viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)

            // ADMIN has null branchId — resolve to first branch
            val resolvedBranchId = branchId ?: run {
                // Admin adding product — they must select a branch
                // For now use first branch, UI will add branch picker later
                sessionManager.currentSession?.let { session ->
                    addProductUseCase.getFirstBranchId(session.businessId)
                }
            }

            if (resolvedBranchId == null) {
                _state.value = state.value.copy(
                    isLoading = false,
                    error = "No branch found. Please create a branch first."
                )
                _eventFlow.emit(UiEvent.Error("No branch found"))
                return@launch
            }

            val cachedPaths = imageUris.mapNotNull { uri ->
                try {
                    imageCacheManager.cacheImage(uri)
                } catch (e: Exception) {
                    Log.e("AddProductViewModel", "Failed to cache image $uri: ${e.message}")
                    null
                }
            }
            val cachedUris = cachedPaths.map { Uri.fromFile(File(it)) }

            val productClientId = UUID.randomUUID().toString()

            val variants = if (addProductRequest.variants.isNullOrEmpty()) {
                listOf(
                    ProductVariantCreateRequest(
                        clientId  = UUID.randomUUID().toString(),
                        sku       = "${addProductRequest.name.take(3).uppercase()}-DEFAULT",
                        salesPrice = addProductRequest.baseSalesPrice,
                        costPrice  = addProductRequest.baseCostPrice,
                        barcode    = barcode,
                        attributes = emptyList(),
                        stock      = addProductRequest.defaultStock
                    )
                )
            } else {
                addProductRequest.variants.map { variant ->
                    variant.copy(clientId = UUID.randomUUID().toString())
                }
            }

            val requestWithIds = addProductRequest.copy(
                clientId  = productClientId,
                branchId  = resolvedBranchId,
                variants  = variants
            )

            when (val result = addProductUseCase(requestWithIds, cachedUris)) {
                is Resource.Success -> {
                    _state.value = state.value.copy(isLoading = false)
                    _eventFlow.emit(UiEvent.Success)
                }
                is Resource.Error -> {
                    _state.value = state.value.copy(
                        isLoading = false,
                        error = result.message ?: "An unexpected error occurred"
                    )
                    _eventFlow.emit(UiEvent.Error(result.message ?: "An unexpected error occurred"))
                }
                is Resource.Loading -> _state.value = state.value.copy(isLoading = true)
            }
        }
    }

    private fun loadBranches() {
        viewModelScope.launch {
            val bId = businessId ?: return@launch
            val branches = branchDao.getActiveBranchesForBusiness(bId)
            _branches.value = branches

            // Auto-select the session branch for staff/manager
            // For admin — they must pick from the dropdown
            val sessionBranchId = sessionManager.currentSession?.branchId
            if (sessionBranchId != null) {
                _selectedBranchId.value = sessionBranchId
            } else if (branches.size == 1) {
                // Only one branch — auto select even for admin
                _selectedBranchId.value = branches.first().id
            }
            // Multiple branches + admin → selectedBranchId stays null until they pick
        }
    }

    sealed class UiEvent {
        data object Success : UiEvent()
        data class Error(val message: String) : UiEvent()
    }

}

data class ProductUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
