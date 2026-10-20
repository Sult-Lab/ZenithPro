package com.techsultan.zenithpro.features.material.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.techsultan.zenithpro.core.components.ZenithButton
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.core.domain.domain.UnitType
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.material.data.local.MaterialEntity
import com.techsultan.zenithpro.features.sales.formatAmount
import org.koin.compose.viewmodel.koinViewModel

data class RecipeIngredientItem(
    val material: MaterialEntity,
    var quantity: Double
)

@Composable
fun RecipeRoute(
    viewModel: RecipeViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RecipeEvent.ShowSuccess -> snackbarHostState.showSnackbar(event.message)
                is RecipeEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val selected = remember(state.products, state.selectedVariantId) {
        state.selectedVariantId?.let { vId ->
            state.products.flatMap { pwv -> pwv.variants.map { pwv.product to it.variant } }
                .firstOrNull { (_, variant) -> variant.id == vId }
        }
    }

    RecipeScreen(
        products = state.products,
        selectedVariantId = state.selectedVariantId,
        selectedProductName = selected?.first?.name,
        selectedProductImageUrl = selected?.first?.imageUrls?.firstOrNull(),
        selectedProductPrice = selected?.second?.salesPrice,
        availableMaterials = state.availableMaterials,
        ingredients = state.ingredients,
        snackbarHostState = snackbarHostState,
        onSelectProduct = { viewModel.selectProduct(it) },
        onChangeProduct = { viewModel.clearProduct() },
        onQuantityChange = { materialId, qty -> viewModel.updateQuantity(materialId, qty) },
        onRemoveMaterial = { viewModel.removeIngredient(it) },
        onAddMaterial = { viewModel.addIngredient(it) },
        onResetToDefault = { viewModel.resetToDefault() },
        onSaveRecipe = { viewModel.saveRecipe() },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    products: List<ProductWithVariants>,
    selectedVariantId: String?,
    selectedProductName: String?,
    selectedProductImageUrl: String?,
    selectedProductPrice: Long?,
    availableMaterials: List<MaterialEntity>,
    ingredients: List<RecipeIngredientItem>,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSelectProduct: (variantId: String) -> Unit,
    onChangeProduct: () -> Unit,
    onQuantityChange: (materialId: String, quantity: Double) -> Unit,
    onRemoveMaterial: (materialId: String) -> Unit,
    onAddMaterial: (MaterialEntity) -> Unit,
    onResetToDefault: () -> Unit,
    onSaveRecipe: () -> Unit,
    onBack: () -> Unit
) {
    var showProductDropdown by remember { mutableStateOf(false) }
    var showAddMaterialPicker by remember { mutableStateOf(false) }

    val totalMaterialCost = remember(ingredients) {
        ingredients.sumOf { (it.material.costPerUnit / 100.0) * it.quantity }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ZenithTopAppBar(
                title = "Product Recipe",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {}
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (selectedVariantId != null) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    ZenithButton(
                        text = "Save Recipe",
                        onClick = onSaveRecipe,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(52.dp),
                        enabled = ingredients.isNotEmpty()
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (selectedVariantId == null) {
                // No product chosen yet — selection lives here, not in a dialog before this screen
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder(enabled = true)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Select a product to build its recipe",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        ExposedDropdownMenuBox(
                            expanded = showProductDropdown,
                            onExpandedChange = { showProductDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = "Select Product",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showProductDropdown) },
                                shape = RoundedCornerShape(12.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = showProductDropdown,
                                onDismissRequest = { showProductDropdown = false }
                            ) {
                                if (products.isEmpty()) {
                                    DropdownMenuItem(text = { Text("No products found") }, onClick = {})
                                } else {
                                    products.forEach { pwv ->
                                        pwv.variants.forEach { vws ->
                                            DropdownMenuItem(
                                                text = { Text("${pwv.product.name} (${vws.variant.sku})") },
                                                onClick = {
                                                    showProductDropdown = false
                                                    onSelectProduct(vws.variant.id)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return@Column
            }

            // Target Product
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(enabled = true)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TARGET PRODUCT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedProductName.orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Selling Price: ₦${(selectedProductPrice ?: 0).formatAmount()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (!selectedProductImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = selectedProductImageUrl,
                                contentDescription = selectedProductName,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = onChangeProduct, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change Product", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ingredients (per 1 unit)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onResetToDefault) {
                    Text("Reset to Default", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (ingredients.isEmpty()) {
                Text(
                    text = "No ingredients yet — add materials below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ingredients.forEach { item ->
                val unitType = UnitType.fromString(item.material.unit)
                val step = if (unitType.isDecimal) 0.1 else 1.0
                val unitCostNaira = item.material.costPerUnit / 100
                val lineCost = (item.material.costPerUnit / 100.0) * item.quantity

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder(enabled = true)
                ) {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.material.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "₦${unitCostNaira.formatAmount()} per ${item.material.unit.lowercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                                IconButton(
                                    onClick = { onQuantityChange(item.material.id, item.quantity - step) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, "Decrease", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    text = Util.formatQuantity(item.quantity, unitType),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.widthIn(min = 32.dp),
                                    textAlign = TextAlign.Center
                                )

                                Text(item.material.unit.lowercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                IconButton(
                                    onClick = { onQuantityChange(item.material.id, item.quantity + step) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, "Increase", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 64.dp, end = 12.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onRemoveMaterial(item.material.id) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, "Remove ${item.material.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "₦${lineCost.toLong().formatAmount()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box {
                OutlinedButton(
                    onClick = { showAddMaterialPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    enabled = availableMaterials.any { m -> ingredients.none { it.material.id == m.id } }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Material", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }

                DropdownMenu(expanded = showAddMaterialPicker, onDismissRequest = { showAddMaterialPicker = false }) {
                    val available = availableMaterials.filter { m -> ingredients.none { it.material.id == m.id } }
                    if (available.isEmpty()) {
                        DropdownMenuItem(text = { Text("All materials already added") }, onClick = {})
                    } else {
                        available.forEach { mat ->
                            DropdownMenuItem(
                                text = { Text("${mat.name} (${mat.unit})") },
                                onClick = {
                                    onAddMaterial(mat)
                                    showAddMaterialPicker = false
                                }
                            )
                        }
                    }
                }
            }

            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cost Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Material Cost", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "₦${totalMaterialCost.toLong().formatAmount()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}