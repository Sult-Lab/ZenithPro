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
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.inventory.data.remote.ProductVariantCreateRequest
import com.techsultan.zenithpro.features.inventory.data.remote.UpdateProductRequest
import com.techsultan.zenithpro.features.inventory.domain.use_case.GetProductUseCase
import com.techsultan.zenithpro.features.inventory.domain.use_case.UpdateProductUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ProductDetailViewModel(
    private val getProductUseCase: GetProductUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val imageCacheManager: ImageCacheManager,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = mutableStateOf(ProductDetailUiState())
    val state: State<ProductDetailUiState> = _state

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _scannedBarcode = mutableStateOf<String?>(null)
    val scannedBarcode: State<String?> = _scannedBarcode

    val businessId: String? get() = try { sessionManager.businessId } catch (e: Exception) { null }

    fun onBarcodeScanned(barcode: String) {
        _scannedBarcode.value = barcode
    }

    fun clearScannedBarcode() {
        _scannedBarcode.value = null
    }

    fun clearState() {
        _state.value = ProductDetailUiState()
        _scannedBarcode.value = null
    }

    fun getProduct(productId: String) {
        viewModelScope.launch {
            _state.value = ProductDetailUiState(
                isLoading = true,
                product = null,
                error = null
            )
            getProductUseCase(productId).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> _state.value = _state.value.copy(isLoading = true)
                    is Resource.Success -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            product = result.data,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun updateProduct(
        updateProductRequest: UpdateProductRequest,
        imageUris: List<Uri>,
        barcode: String? = null
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val remoteUrls = imageUris.filter { it.toString().startsWith("http") }.map { it.toString() }
            val localUris = imageUris.filter { !it.toString().startsWith("http") }

            val cachedLocalUris = localUris.mapNotNull { uri ->
                try {
                    val path = imageCacheManager.cacheImage(uri)
                    Uri.fromFile(File(path))
                } catch (e: Exception) {
                    Log.e("ProductDetailVM", "Failed to cache image $uri: ${e.message}")
                    null
                }
            }

            val existingDefaultVariant = state.value.product
                ?.variants
                ?.firstOrNull()
            // If no variants were added by the user, create a single default variant
            val variants = if (updateProductRequest.variants.isEmpty()) {

                val existingSku = existingDefaultVariant?.variant?.sku
                val existingId = existingDefaultVariant?.variant?.id

                listOf(
                    ProductVariantCreateRequest(
                        clientId = existingId ?: UUID.randomUUID().toString(),
                        sku = existingSku ?: "${updateProductRequest.name.take(3).uppercase()}-DEFAULT",
                        salesPrice = updateProductRequest.baseSalesPrice,
                        costPrice = updateProductRequest.baseCostPrice,
                        barcode = barcode,
                        attributes = emptyList(),
                        stock = updateProductRequest.defaultStock
                    )
                )
            } else {
                updateProductRequest.variants.map { variant ->
                    if (variant.clientId.isEmpty()) variant.copy(clientId = UUID.randomUUID().toString())
                    else variant
                }
            }

            val requestWithIds = updateProductRequest.copy(
                variants = variants,
                imageUrls = remoteUrls
            )

            when (val result = updateProductUseCase(requestWithIds, cachedLocalUris)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(isLoading = false)
                    _eventFlow.emit(UiEvent.Success)
                    // Refresh product
                    getProduct(updateProductRequest.clientId)
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message ?: "An unexpected error occurred"
                    )
                    _eventFlow.emit(UiEvent.Error(result.message ?: "An unexpected error occurred"))
                }
                is Resource.Loading -> _state.value = _state.value.copy(isLoading = true)
            }
        }
    }

    sealed class UiEvent {
        data object Success : UiEvent()
        data class Error(val message: String) : UiEvent()
    }
}

data class ProductDetailUiState(
    val isLoading: Boolean = false,
    val product: ProductWithVariants? = null,
    val error: String? = null
)
