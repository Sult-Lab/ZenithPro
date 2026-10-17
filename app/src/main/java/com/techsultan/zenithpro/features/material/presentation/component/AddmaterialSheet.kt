package com.techsultan.zenithpro.features.material.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techsultan.zenithpro.core.components.CustomTextField
import com.techsultan.zenithpro.core.components.ZenithButton
import com.techsultan.zenithpro.core.domain.domain.UnitType
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.core.util.Util.trimOrNull
import com.techsultan.zenithpro.features.material.data.local.MaterialEntity
import com.techsultan.zenithpro.features.material.data.remote.UpsertMaterialRequest
import java.util.UUID

@Composable
fun MaterialItemCard(
    material: MaterialEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onAdjust: () -> Unit,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val isOutOfStock = material.status == "OUT_OF_STOCK" || material.quantity <= 0
    val isLowStock = material.status == "LOW_STOCK" || material.isLowStock

    val accentBarColor = when {
        isOutOfStock -> Color(0xFFE53935)
        isLowStock -> Color(0xFFFF9800)
        else -> null
    }

    val unitType = UnitType.fromString(material.unit)
    val formattedQty = Util.formatQuantity(material.quantity, unitType)
    val costNaira = material.costPerUnit / 100.0
    val formattedPrice = "₦ " + String.format(java.util.Locale.US, "%,.2f", costNaira).removeSuffix(".00")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Accent edge indicator for low/out of stock
            if (accentBarColor != null) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(accentBarColor)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image Thumbnail Container
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Material Details Column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = material.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedPrice,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " / ${material.unit.lowercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    material.supplier?.let { supplier ->
                        Text(
                            text = supplier,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Badge & Options Menu
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Adjust stock") },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onAdjust()
                                }
                            )
                            if (isOutOfStock) {
                                DropdownMenuItem(
                                    text = { Text("Mark Available") },
                                    leadingIcon = { Icon(Icons.Default.CheckCircleOutline, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onStatusChange("AVAILABLE")
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Mark Out of Stock") },
                                    leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onStatusChange("OUT_OF_STOCK")
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Badge Styling
                    when {
                        isOutOfStock -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFEBEE)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = Color(0xFFC62828),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Out of Stock",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828)
                                    )
                                }
                            }
                        }
                        isLowStock -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF3E0)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$formattedQty left",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = "$formattedQty in stock",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMaterialSheet(
    existing: MaterialEntity?,
    onSave: (request: UpsertMaterialRequest, initialStock: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var unit by remember { mutableStateOf(existing?.unit ?: "UNITS") }
    var costPerUnit by remember {
        mutableStateOf(if (existing != null) (existing.costPerUnit / 100.0).toString() else "")
    }
    var initialStock by remember {
        mutableStateOf(existing?.quantity?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "")
    }
    var lowStock by remember {
        mutableStateOf(existing?.lowStockAlert?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "")
    }
    var supplier by remember { mutableStateOf(existing?.supplier ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var showUnitMenu by remember { mutableStateOf(false) }

    val selectedUnitType = UnitType.fromString(unit)

  //  val units = listOf("KG", "GRAMS", "LITRES", "ML", "PIECES", "BAGS", "CARTONS", "METRES", "UNITS")
    val unitGroups = mapOf(
        "Discrete" to listOf(
            UnitType.UNIT,
            UnitType.PIECE,
            UnitType.PACK,
            UnitType.DOZEN,
            UnitType.CARTON,
            UnitType.BAG,
            UnitType.SACHET,
            UnitType.BOTTLE,
            UnitType.TIN,
            UnitType.ROLL,
            UnitType.BUNDLE,
            UnitType.PALLET
        ),
        "Weight" to listOf(UnitType.KILOGRAM, UnitType.GRAM, UnitType.POUND),
        "Volume" to listOf(UnitType.LITRE, UnitType.MILLILITRE, UnitType.GALLON),
        "Length" to listOf(UnitType.METRE, UnitType.YARD, UnitType.FOOT),
    )

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
                text = if (existing != null) "Edit Material" else "Add New Material",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Material Name
            CustomTextField(
                value = name,
                onValueChange = { name = it },
                label = "Material Name",
                placeholder = "e.g., Timber, Flour"
            )

            // Unit of Measurement Dropdown
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Unit of Measurement",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                ExposedDropdownMenuBox(
                    expanded = showUnitMenu,
                    onExpandedChange = { showUnitMenu = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedUnitType.label} (${selectedUnitType.abbreviation})",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showUnitMenu) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF9FAFB),
                            focusedContainerColor = Color(0xFFF9FAFB),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    ExposedDropdownMenu(
                        expanded = showUnitMenu,
                        onDismissRequest = { showUnitMenu = false }
                    ) {
                        unitGroups.forEach { (groupName, units) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        groupName.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {},
                                enabled = false
                            )
                            units.forEach { unitItem ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(unitItem.label)
                                            Text(
                                                unitItem.abbreviation,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        unit = unitItem.name
                                        showUnitMenu = false
                                    },
                                    leadingIcon = if (unitItem == selectedUnitType) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                null,
                                                tint = Color(0xFF00C853)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // Cost per Unit
            CustomTextField(
                value = costPerUnit,
                onValueChange = { costPerUnit = it },
                label = "Cost per Unit",
                placeholder = "0.00",
                prefix = "₦ ",
                keyboardType = KeyboardType.Decimal
            )

            // Initial Stock & Alert Level Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CustomTextField(
                    value = initialStock,
                    onValueChange = { initialStock = it },
                    label = if (existing != null) "Stock Quantity" else "Initial Stock",
                    placeholder = "0",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )

                CustomTextField(
                    value = lowStock,
                    onValueChange = { lowStock = it },
                    label = "Alert Level",
                    placeholder = "e.g. 10",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }

            // Information Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEBF3FF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "We'll send you a notification when your stock drops below the alert level.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1E3A8A),
                        lineHeight = 18.sp
                    )
                }
            }

            // Optional Supplier
            CustomTextField(
                value = supplier,
                onValueChange = { supplier = it },
                label = "Supplier (Optional)",
                placeholder = "e.g., Dangote Sugar"
            )

            // Optional Notes
            CustomTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Notes (Optional)",
                placeholder = "Additional details or notes",
                minLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            ZenithButton(
                text = if (existing != null) "Update Material" else "Save Material",
                onClick = {
                    if (name.isNotBlank()) {
                        val materialId = existing?.id ?: UUID.randomUUID().toString()
                        val parsedCost = costPerUnit.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
                        val parsedAlert = lowStock.toDoubleOrNull()
                        val initialStockDouble = initialStock.toDoubleOrNull() ?: 0.0

                        val request = UpsertMaterialRequest(
                            id = materialId,
                            name = name.trim(),
                            description = description.trimOrNull(),
                            unit = unit,
                            costPerUnit = parsedCost,
                            lowStockAlert = parsedAlert,
                            supplier = supplier.trimOrNull(),
                            notes = notes.trimOrNull()
                        )
                        onSave(request, initialStockDouble)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustStockSheet(
    material: MaterialEntity,
    onConfirm: (Double, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isAdd by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Adjust Stock — ${material.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val unitType = UnitType.fromString(material.unit)
            val currentStockText = Util.formatReceiptQuantity(material.quantity, unitType)

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Current Stock: $currentStockText",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(true to "Add Stock", false to "Remove Stock").forEach { (add, label) ->
                    val isSelected = isAdd == add
                    OutlinedButton(
                        onClick = { isAdd = add },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (isSelected) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = if (add) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                contentColor = if (add) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            CustomTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = "Quantity",
                placeholder = "0",
                suffix = material.unit,
                keyboardType = KeyboardType.Decimal
            )

            CustomTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Reason / Notes (Optional)",
                placeholder = "e.g., New delivery, damaged stock"
            )

            ZenithButton(
                text = "Confirm Adjustment",
                onClick = {
                    val qty = amount.toDoubleOrNull() ?: return@ZenithButton
                    onConfirm(if (isAdd) qty else -qty, notes.trimOrNull())
                },
                containerColor = if (isAdd) Color(0xFF2E7D32) else Color(0xFFC62828),
                contentColor = Color.White
            )
        }
    }
}