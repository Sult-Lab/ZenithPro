package com.techsultan.zenithpro.features.product.presentation

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.util.ImageCacheManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.product.data.remote.AddProductRequest
import com.techsultan.zenithpro.features.product.data.remote.ProductVariantCreateRequest
import com.techsultan.zenithpro.features.product.domain.use_case.AddProductUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class AddProductViewModel(
    private val addProductUseCase: AddProductUseCase,
    private val imageCacheManager: ImageCacheManager
) : ViewModel() {

    private val _state = mutableStateOf(ProductUiState())
    val state: State<ProductUiState> = _state

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun addProduct(
        addProductRequest: AddProductRequest,
        imageUris: List<Uri>
    ) {
        viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)

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

            // If no variants were added by the user, create a single default variant
            // so stock information is never lost
            val variants = if (addProductRequest.variants.isNullOrEmpty()) {
                listOf(
                    ProductVariantCreateRequest(
                        clientId = UUID.randomUUID().toString(),
                        sku = "${addProductRequest.name.take(3).uppercase()}-DEFAULT",
                        salesPrice = addProductRequest.baseSalesPrice,
                        costPrice = addProductRequest.baseCostPrice,
                        barcode = null,
                        attributes = emptyList(),   // no attributes = simple product
                        stock = addProductRequest.defaultStock   // ← see fix below
                    )
                )
            } else {
                addProductRequest.variants.map { variant ->
                    variant.copy(clientId = UUID.randomUUID().toString())
                }
            }

            val requestWithIds = addProductRequest.copy(
                clientId = productClientId,
                variants = variants
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
                    Log.e("AddProductViewModel", "addProduct: ${result.message}")
                    _eventFlow.emit(UiEvent.Error(result.message ?: "An unexpected error occurred"))
                }
                is Resource.Loading -> _state.value = state.value.copy(isLoading = true)
            }
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