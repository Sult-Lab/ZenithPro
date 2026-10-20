package com.techsultan.zenithpro.features.material.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.domain.domain.UnitType
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.inventory.domain.use_case.GetProductsUseCase
import com.techsultan.zenithpro.features.material.data.local.MaterialEntity
import com.techsultan.zenithpro.features.material.data.remote.RecipeItem
import com.techsultan.zenithpro.features.material.domain.repository.MaterialRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.round

sealed interface RecipeEvent {
    data class ShowSuccess(val message: String) : RecipeEvent
    data class ShowError(val message: String) : RecipeEvent
}

class RecipeViewModel(
    private val materialRepository: MaterialRepository,
    private val getProductsUseCase: GetProductsUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeUiState())
    val state: StateFlow<RecipeUiState> = _state.asStateFlow()

    private val _events = Channel<RecipeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val businessId = sessionManager.businessId ?: return@launch
            getProductsUseCase(businessId).collect { result ->
                if (result is Resource.Success) {
                    _state.update { it.copy(products = result.data ?: emptyList()) }
                }
            }
        }
    }

    fun selectProduct(variantId: String) {
        _state.update {
            it.copy(selectedVariantId = variantId, ingredients = emptyList(), isLoadingRecipe = true)
        }

        viewModelScope.launch {
            materialRepository.getRecipeForVariant(variantId).collect { result ->
                if (result is Resource.Success) {
                    _state.update {
                        it.copy(
                            ingredients = (result.data ?: emptyList()).map { rwm ->
                                RecipeIngredientItem(material = rwm.material, quantity = rwm.recipe.quantityNeeded)
                            },
                            isLoadingRecipe = false
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            val businessId = sessionManager.businessId ?: return@launch
            materialRepository.getMaterials(businessId, status = null).collect { result ->
                if (result is Resource.Success) {
                    _state.update { it.copy(availableMaterials = result.data ?: emptyList()) }
                }
            }
        }
    }

    fun clearProduct() {
        _state.update {
            it.copy(selectedVariantId = null, ingredients = emptyList(), availableMaterials = emptyList())
        }
    }

    fun updateQuantity(materialId: String, quantity: Double) {
        _state.update { s ->
            s.copy(ingredients = s.ingredients.map {
                if (it.material.id == materialId) {
                    val roundedQty = (round(quantity * 100.0) / 100.0).coerceAtLeast(0.0)
                    it.copy(quantity = roundedQty)
                } else it
            })
        }
    }

    fun removeIngredient(materialId: String) {
        _state.update { s -> s.copy(ingredients = s.ingredients.filterNot { it.material.id == materialId }) }
    }

    fun addIngredient(material: MaterialEntity) {
        _state.update { s ->
            if (s.ingredients.any { it.material.id == material.id }) return@update s
            val initialQty = if (UnitType.fromString(material.unit).isDecimal) 0.1 else 1.0
            s.copy(ingredients = s.ingredients + RecipeIngredientItem(material, initialQty))
        }
    }

    // Discards unsaved edits, re-fetches the last saved recipe
    fun resetToDefault() {
        state.value.selectedVariantId?.let { selectProduct(it) }
    }

    fun saveRecipe() {
        val vId = state.value.selectedVariantId ?: return
        viewModelScope.launch {
            val businessId = sessionManager.businessId ?: return@launch
            val result = materialRepository.saveRecipe(
                variantId = vId,
                items = state.value.ingredients.map {
                    RecipeItem(materialId = it.material.id, quantityNeeded = it.quantity)
                },
                businessId = businessId
            )
            when (result) {
                is Resource.Success -> {
                    clearProduct()
                    _events.send(RecipeEvent.ShowSuccess("Recipe saved successfully"))
                }
                is Resource.Error -> {
                    _events.send(RecipeEvent.ShowError(result.message ?: "Failed to save recipe"))
                }
                is Resource.Loading -> {}
            }
        }
    }
}

data class RecipeUiState(
    val products: List<ProductWithVariants> = emptyList(),
    val selectedVariantId: String? = null,
    val ingredients: List<RecipeIngredientItem> = emptyList(),
    val availableMaterials: List<MaterialEntity> = emptyList(),
    val isLoadingRecipe: Boolean = false
)