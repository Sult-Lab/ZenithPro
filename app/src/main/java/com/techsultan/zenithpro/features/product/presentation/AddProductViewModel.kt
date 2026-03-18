package com.techsultan.zenithpro.features.product.presentation

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.product.data.remote.AddProductRequest
import com.techsultan.zenithpro.features.product.domain.use_case.AddProductUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AddProductViewModel(
    private val addProductUseCase: AddProductUseCase,
) : ViewModel() {

    private val _state = mutableStateOf(ProductUiState())
    val state: State<ProductUiState> = _state

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun addProduct(
        addProductRequest: AddProductRequest,
        imageUris: List<Uri>
    ){
        viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)

            val requestWithIds = addProductRequest.copy(
                clientId = UUID.randomUUID().toString(),
                variants = addProductRequest.variants?.map { variant ->
                    variant.copy(clientId = UUID.randomUUID().toString())
                }
            )

            when(val result = addProductUseCase(addProductRequest = requestWithIds, imageUris)) {
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
                is Resource.Loading -> {
                    _state.value = state.value.copy(isLoading = true)
                }
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