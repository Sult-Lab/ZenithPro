package com.techsultan.zenithpro.features.production.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.techsultan.zenithpro.core.components.CustomTextField
import com.techsultan.zenithpro.core.components.ZenithButton
import com.techsultan.zenithpro.core.domain.domain.UnitType
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.core.util.Util.trimOrNull
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.material.data.local.MaterialEntity
import com.techsultan.zenithpro.features.material.data.local.RecipeWithMaterial
import java.util.Locale

data class RequiredMaterialItem(
    val material: MaterialEntity,
    var requiredQty: Double,
    val quantityPerUnit: Double,   // from the recipe; null-equivalent 0.0 for a manually-added extra
    var isOverridden: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProductionOrderSheet(
    products: List<ProductWithVariants>,
    materials: List<MaterialEntity>,
    recipeMaterials: List<RecipeWithMaterial>,   // reactively supplied by the ViewModel for the selected variant
    onProductSelected: (variantId: String) -> Unit,
    onSave: (variantId: String, quantity: Double, notes: String?, materials: List<RequiredMaterialItem>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedVariantId by remember { mutableStateOf("") }
    var selectedProductName by remember { mutableStateOf("Select Product") }
    var quantityInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var showProductDropdown by remember { mutableStateOf(false) }
    var showAddMaterialPicker by remember { mutableStateOf(false) }

    val selectedMaterials = remember { mutableStateListOf<RequiredMaterialItem>() }

    val selectedProduct = remember(products, selectedVariantId) {
        if (selectedVariantId.isBlank()) null
        else products.firstOrNull { pwv ->
            pwv.variants.any { it.variant.id == selectedVariantId }
        }?.product
    }

    val selectedUnitType = remember(selectedProduct) {
        UnitType.fromString(selectedProduct?.unitType)
    }

    val productUnitSuffix = remember(selectedUnitType, selectedProduct) {
        if (selectedProduct == null) "Units"
        else selectedUnitType.abbreviation.ifBlank { selectedUnitType.label }
    }

    // Rebuild the materials list from the recipe whenever the product changes,
    // or the recipe for the currently selected product finishes loading.
    LaunchedEffect(recipeMaterials, selectedVariantId) {
        if (selectedVariantId.isNotBlank()) {
            val qty = quantityInput.toDoubleOrNull() ?: 0.0
            selectedMaterials.clear()
            recipeMaterials.forEach { rwm ->
                selectedMaterials.add(
                    RequiredMaterialItem(
                        material = rwm.material,
                        requiredQty = rwm.recipe.quantityNeeded * qty,
                        quantityPerUnit = rwm.recipe.quantityNeeded
                    )
                )
            }
        }
    }

    // Auto-rescale any line the user hasn't manually overridden when the planned quantity changes.
    fun rescaleToQuantity(qty: Double) {
        for (i in selectedMaterials.indices) {
            val item = selectedMaterials[i]
            if (!item.isOverridden) {
                selectedMaterials[i] = item.copy(requiredQty = item.quantityPerUnit * qty)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "New Production Order",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Select Product
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Product",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = showProductDropdown,
                    onExpandedChange = { showProductDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedProductName,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showProductDropdown) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF9FAFB),
                            focusedContainerColor = Color(0xFFF9FAFB),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    ExposedDropdownMenu(
                        expanded = showProductDropdown,
                        onDismissRequest = { showProductDropdown = false }
                    ) {
                        if (products.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No products found") },
                                onClick = { showProductDropdown = false }
                            )
                        } else {
                            products.forEach { productWithVar ->
                                productWithVar.variants.forEach { varWithStock ->
                                    DropdownMenuItem(
                                        text = { Text("${productWithVar.product.name} (${varWithStock.variant.sku})") },
                                        onClick = {
                                            selectedVariantId = varWithStock.variant.id
                                            selectedProductName = productWithVar.product.name
                                            showProductDropdown = false
                                            onProductSelected(varWithStock.variant.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Production Quantity
            CustomTextField(
                value = quantityInput,
                onValueChange = { newVal ->
                    quantityInput = newVal
                    newVal.toDoubleOrNull()?.let { rescaleToQuantity(it) }
                },
                label = "Production Quantity",
                placeholder = "50",
                suffix = productUnitSuffix,
                keyboardType = KeyboardType.Decimal
            )

            // Raw Materials Needed
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Raw Materials Needed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(onClick = {
                        quantityInput.toDoubleOrNull()?.let { qty ->
                            selectedMaterials.clear()
                            recipeMaterials.forEach { rwm ->
                                selectedMaterials.add(
                                    RequiredMaterialItem(
                                        material = rwm.material,
                                        requiredQty = rwm.recipe.quantityNeeded * qty,
                                        quantityPerUnit = rwm.recipe.quantityNeeded
                                    )
                                )
                            }
                        }
                    }) {
                        Text(
                            text = "Reset to Default",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Adjust quantities below if actual usage differs from recipe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (selectedVariantId.isBlank()) {
                    Text(
                        text = "Select a product to load its recipe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (selectedMaterials.isEmpty()) {
                    Text(
                        text = "No recipe materials for this product — add any needed below, or continue with none (e.g. a service with no material cost).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                selectedMaterials.forEachIndexed { index, reqItem ->
                    val isLowStock = reqItem.requiredQty > reqItem.material.quantity

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(enabled = true)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reqItem.material.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Current Stock: ${Util.formatQuantity(reqItem.material.quantity, UnitType.fromString(reqItem.material.unit))} ${reqItem.material.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isLowStock) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isLowStock) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                                ) {
                                    Text(
                                        text = if (isLowStock) "Low Stock" else "In Stock",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLowStock) Color(0xFFC62828) else Color(0xFF2E7D32)
                                    )
                                }

                                IconButton(onClick = { selectedMaterials.removeAt(index) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove ${reqItem.material.name}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Required:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                CustomTextField(
                                    value = reqItem.requiredQty.toString(),
                                    onValueChange = { newVal ->
                                        newVal.toDoubleOrNull()?.let {
                                            selectedMaterials[index] = reqItem.copy(requiredQty = it, isOverridden = true)
                                        }
                                    },
                                    label = "",
                                    placeholder = "0",
                                    suffix = reqItem.material.unit,
                                    keyboardType = KeyboardType.Decimal,
                                    modifier = Modifier.width(140.dp)
                                )
                            }

                            if (isLowStock) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Warning: Required quantity exceeds available stock.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }

                Box {
                    OutlinedButton(
                        onClick = { showAddMaterialPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = materials.any { m -> selectedMaterials.none { it.material.id == m.id } }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Add Extra Material", style = MaterialTheme.typography.labelLarge)
                    }

                    DropdownMenu(
                        expanded = showAddMaterialPicker,
                        onDismissRequest = { showAddMaterialPicker = false }
                    ) {
                        val available = materials.filter { m -> selectedMaterials.none { it.material.id == m.id } }
                        if (available.isEmpty()) {
                            DropdownMenuItem(text = { Text("All materials already added") }, onClick = {})
                        } else {
                            available.forEach { mat ->
                                DropdownMenuItem(
                                    text = { Text("${mat.name} (${mat.unit})") },
                                    onClick = {
                                        selectedMaterials.add(
                                            RequiredMaterialItem(
                                                material = mat,
                                                requiredQty = 0.0,
                                                quantityPerUnit = 0.0,
                                                isOverridden = true // not recipe-scaled, so quantity changes stay manual
                                            )
                                        )
                                        showAddMaterialPicker = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Estimated Cost
            val estimatedCostTotal = remember(selectedMaterials.toList()) {
                selectedMaterials.sumOf { (it.material.costPerUnit / 100.0) * it.requiredQty }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEBF3FF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ESTIMATED COST",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₦ ${String.format(Locale.US, "%,.2f", estimatedCostTotal)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Includes raw materials only. Overheads not included.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1E3A8A).copy(alpha = 0.8f)
                    )
                }
            }

            CustomTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = "Notes (Optional)",
                placeholder = "Add batch notes or special instructions"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", style = MaterialTheme.typography.titleMedium)
                }

                ZenithButton(
                    text = "Start Production",
                    onClick = {
                        val qty = quantityInput.toDoubleOrNull() ?: return@ZenithButton
                        if (selectedVariantId.isNotBlank() && qty > 0) {
                            onSave(selectedVariantId, qty, notesInput.trimOrNull(), selectedMaterials.toList())
                        }
                    },
                    modifier = Modifier.weight(1.2f).height(52.dp),
                    icon = Icons.Default.Factory
                )
            }
        }
    }
}
