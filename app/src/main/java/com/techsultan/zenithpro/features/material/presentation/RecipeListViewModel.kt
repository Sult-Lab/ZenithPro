package com.techsultan.zenithpro.features.material.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.manager.SessionManager
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.inventory.domain.use_case.GetProductsUseCase
import com.techsultan.zenithpro.features.material.data.local.RecipeWithMaterial
import com.techsultan.zenithpro.features.material.domain.repository.MaterialRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface RecipeStockStatus {
    data class InStock(val maxProducibleUnits: Int) : RecipeStockStatus
    data class LowStock(val maxProducibleUnits: Int, val lowMaterialName: String) : RecipeStockStatus
    data class Blocked(val blockedMaterialName: String) : RecipeStockStatus
}

data class RecipeItemUiState(
    val variantId: String,
    val productId: String,
    val productName: String,
    val variantSku: String,
    val category: String?,
    val imageUrl: String?,
    val unitCost: Double,
    val sellingPrice: Long,
    val marginPercent: Double,
    val ingredientsCount: Int,
    val ingredientsSummary: String,
    val stockStatus: RecipeStockStatus
)

data class RecipeSummaryStats(
    val totalRecipes: Int = 0,
    val avgMarginPercent: Double = 0.0,
    val readyToProduceCount: Int = 0
)

data class RecipeListUiState(
    val allRecipes: List<RecipeItemUiState> = emptyList(),
    val filteredRecipes: List<RecipeItemUiState> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val summaryStats: RecipeSummaryStats = RecipeSummaryStats()
)

class RecipeListViewModel(
    private val materialRepository: MaterialRepository,
    private val getProductsUseCase: GetProductsUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeListUiState(isLoading = true))
    val state: StateFlow<RecipeListUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            val session = sessionManager.loadSession()
            val bId = session?.businessId ?: sessionManager.businessId
            if (bId.isNotBlank()) {
                observeData(bId)
            }
        }
    }

    private fun observeData(businessId: String) {
        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            combine(
                getProductsUseCase(businessId),
                materialRepository.getAllRecipes(businessId)
            ) { productsRes, recipesRes ->
                productsRes to recipesRes
            }.collect { (productsRes, recipesRes) ->
                Log.d("RecipeListViewModel", "observeData: $recipesRes")
                val current = _state.value

                val products = (productsRes as? Resource.Success)?.data
                val recipes = (recipesRes as? Resource.Success)?.data

                // Don't replace valid data with empty lists during loading/errors.
                if (products == null || recipes == null) {
                    val isInitialLoad = current.allRecipes.isEmpty()

                    _state.update {
                        it.copy(
                            isLoading = isInitialLoad,
                            error = if (
                                productsRes is Resource.Error ||
                                recipesRes is Resource.Error
                            ) {
                                "Unable to refresh recipes. Showing available data."
                            } else {
                                it.error
                            }
                        )
                    }
                    return@collect
                }

                val items = buildRecipeItems(products, recipes)

                val categories = items
                    .mapNotNull { it.category }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()

                val selectedCategory = current.selectedCategory
                    ?.takeIf { it in categories }

                _state.update {
                    it.copy(
                        allRecipes = items,
                        filteredRecipes = filterRecipes(
                            items,
                            current.searchQuery,
                            selectedCategory
                        ),
                        categories = categories,
                        selectedCategory = selectedCategory,
                        searchQuery = current.searchQuery,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                        summaryStats = RecipeSummaryStats(
                            totalRecipes = items.size,
                            avgMarginPercent = items
                                .map { it.marginPercent }
                                .average()
                                .takeIf { items.isNotEmpty() } ?: 0.0,
                            readyToProduceCount = items.count {
                                it.stockStatus !is RecipeStockStatus.Blocked
                            }
                        )
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val bId = sessionManager.businessId
            if (bId.isNotBlank()) {
                _state.update { it.copy(isRefreshing = true) }
                materialRepository.pullFromServer(bId)
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun loadData() {
        if (loadJob == null || loadJob?.isActive == false) {
            val bId = sessionManager.businessId
            if (bId.isNotBlank()) {
                observeData(bId)
            }
        }
    }

    private fun buildRecipeItems(
        products: List<ProductWithVariants>,
        recipes: List<RecipeWithMaterial>
    ): List<RecipeItemUiState> {
        val recipeGroupMap = recipes.groupBy { it.recipe.variantId }
        val items = mutableListOf<RecipeItemUiState>()

        products.forEach { productWithVariants ->
            val product = productWithVariants.product

            productWithVariants.variants.forEach { variantWithStock ->
                val variant = variantWithStock.variant
                val recipeItems = recipeGroupMap[variant.id].orEmpty()

                // Only display variants that have recipe ingredients.
                if (recipeItems.isEmpty()) return@forEach

                val unitCost = recipeItems.sumOf { item ->
                    (item.material.costPerUnit / 100.0) *
                            item.recipe.quantityNeeded
                }

                val sellingPrice = variant.salesPrice
                    .takeIf { it > 0.0 }
                    ?: product.baseSalesPrice

                val marginPercent = if (sellingPrice > 0.0) {
                    ((sellingPrice - unitCost) / sellingPrice) * 100.0
                } else {
                    0.0
                }

                // Calculate how many complete product units can be made
                // from the available quantity of each required material.
                val producibleUnits = recipeItems.map { item ->
                    val needed = item.recipe.quantityNeeded
                    val available = item.material.quantity

                    if (needed > 0.0) {
                        (available / needed).toInt()
                    } else {
                        Int.MAX_VALUE
                    }
                }

                val minProducible = producibleUnits.minOrNull() ?: 0

                val outOfStockMaterial = recipeItems.firstOrNull { item ->
                    item.material.quantity <= 0.0 ||
                            (
                                    item.recipe.quantityNeeded > 0.0 &&
                                            (item.material.quantity /
                                                    item.recipe.quantityNeeded).toInt() <= 0
                                    )
                }

                val lowStockMaterial = recipeItems.firstOrNull { item ->
                    item.material.isLowStock ||
                            (
                                    item.material.lowStockAlert != null &&
                                            item.material.quantity <=
                                            item.material.lowStockAlert
                                    )
                }

                val stockStatus = when {
                    outOfStockMaterial != null -> {
                        RecipeStockStatus.Blocked(
                            outOfStockMaterial.material.name
                        )
                    }

                    lowStockMaterial != null || minProducible < 20 -> {
                        RecipeStockStatus.LowStock(
                            maxProducibleUnits = minProducible,
                            lowMaterialName =
                                lowStockMaterial?.material?.name ?: "Materials"
                        )
                    }

                    else -> {
                        RecipeStockStatus.InStock(minProducible)
                    }
                }

                val ingredientsSummary = buildString {
                    append(
                        recipeItems
                            .take(5)
                            .joinToString(", ") { it.material.name }
                    )

                    if (recipeItems.size > 5) {
                        append("...")
                    }
                }

                items.add(
                    RecipeItemUiState(
                        variantId = variant.id,
                        productId = product.id,
                        productName = product.name,
                        variantSku = variant.sku,
                        category = product.category,
                        imageUrl = product.imageUrls.firstOrNull(),
                        unitCost = unitCost,
                        sellingPrice = sellingPrice,
                        marginPercent = marginPercent,
                        ingredientsCount = recipeItems.size,
                        ingredientsSummary = ingredientsSummary,
                        stockStatus = stockStatus
                    )
                )
            }
        }

        return items
    }

    fun onSearchQueryChanged(query: String) {
        _state.update {
            it.copy(
                searchQuery = query,
                filteredRecipes = filterRecipes(it.allRecipes, query, it.selectedCategory)
            )
        }
    }

    fun onCategorySelected(category: String?) {
        _state.update {
            it.copy(
                selectedCategory = category,
                filteredRecipes = filterRecipes(it.allRecipes, it.searchQuery, category)
            )
        }
    }

    private fun filterRecipes(
        recipes: List<RecipeItemUiState>,
        query: String,
        category: String?
    ): List<RecipeItemUiState> {
        return recipes.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.productName.contains(query, ignoreCase = true) ||
                    item.variantSku.contains(query, ignoreCase = true) ||
                    item.ingredientsSummary.contains(query, ignoreCase = true)

            val matchesCategory = category == null || item.category.equals(category, ignoreCase = true)

            matchesQuery && matchesCategory
        }
    }
}
