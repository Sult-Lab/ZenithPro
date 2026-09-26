package com.techsultan.zenithpro.features.production.presentation.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import java.util.Locale

data class RequiredMaterialItem(
    val material: MaterialEntity,
    var requiredQty: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProductionOrderSheet(
    products: List<ProductWithVariants>,
    materials: List<MaterialEntity>,
    onSave: (variantId: String, quantity: Double, notes: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedVariantId by remember { mutableStateOf("") }
    var selectedProductName by remember { mutableStateOf("Select Product") }
    var quantityInput by remember { mutableStateOf("50") }
    var notesInput by remember { mutableStateOf("") }
    var showProductDropdown by remember { mutableStateOf(false) }

    // Required Materials list
    val selectedMaterials = remember { mutableStateListOf<RequiredMaterialItem>() }

    LaunchedEffect(materials) {
        if (selectedMaterials.isEmpty() && materials.isNotEmpty()) {
            materials.take(3).forEach { mat ->
                selectedMaterials.add(RequiredMaterialItem(mat, 2.5))
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

            // Select Product Dropdown
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                                text = { Text("No products found (type custom ID below)") },
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
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedVariantId.isBlank()) {
                CustomTextField(
                    value = selectedVariantId,
                    onValueChange = { selectedVariantId = it },
                    label = "Or enter Variant ID directly",
                    placeholder = "e.g. VAR-10293"
                )
            }

            // Production Quantity
            CustomTextField(
                value = quantityInput,
                onValueChange = { quantityInput = it },
                label = "Production Quantity",
                placeholder = "50",
                suffix = "Units",
                keyboardType = KeyboardType.Decimal
            )

            // Raw Materials Needed Section
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

                    TextButton(onClick = { /* Reset to default */ }) {
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

                Spacer(modifier = Modifier.height(4.dp))

                // Material Items
                selectedMaterials.forEachIndexed { index, reqItem ->
                    val isLowStock = reqItem.requiredQty > reqItem.material.quantity

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(enabled = true)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
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
                                            selectedMaterials[index] = reqItem.copy(requiredQty = it)
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

                OutlinedButton(
                    onClick = {
                        if (materials.isNotEmpty()) {
                            val nextMat = materials.getOrNull(selectedMaterials.size % materials.size)
                            if (nextMat != null) {
                                selectedMaterials.add(RequiredMaterialItem(nextMat, 1.0))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Extra Material",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Estimated Cost Banner Card
            val estimatedCostTotal = remember(selectedMaterials) {
                selectedMaterials.sumOf { (it.material.costPerUnit / 100.0) * it.requiredQty }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEBF3FF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
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

            // Optional Notes
            CustomTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = "Notes (Optional)",
                placeholder = "Add batch notes or special instructions"
            )

            // Bottom Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", style = MaterialTheme.typography.titleMedium)
                }

                ZenithButton(
                    text = "Start Production",
                    onClick = {
                        val qty = quantityInput.toDoubleOrNull() ?: return@ZenithButton
                        if (selectedVariantId.isNotBlank() && qty > 0) {
                            onSave(selectedVariantId, qty, notesInput.trimOrNull())
                        }
                    },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(52.dp),
                    icon = Icons.Default.Factory
                )
            }
        }
    }
}
