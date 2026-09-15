package com.techsultan.zenithpro.features.material.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.domain.domain.UnitType
import com.techsultan.zenithpro.core.util.Util.trimOrNull
import com.techsultan.zenithpro.features.material.data.local.MaterialEntity
import com.techsultan.zenithpro.features.material.data.remote.UpsertMaterialRequest
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialScreen(
    viewModel: MaterialViewModel = koinViewModel(),
    onMaterialClick: (MaterialEntity) -> Unit,
    onNewProduction: () -> Unit,
    onBack: () -> Unit
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingMat by remember { mutableStateOf<MaterialEntity?>(null) }
    var adjustingMat  by remember { mutableStateOf<MaterialEntity?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MaterialViewModel.MaterialEvent.Saved ->
                    snackbarHost.showSnackbar("Material saved")
                is MaterialViewModel.MaterialEvent.StockAdjusted ->
                    snackbarHost.showSnackbar("Stock updated")
                is MaterialViewModel.MaterialEvent.ShowError ->
                    snackbarHost.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Materials") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFloatingActionButton(onClick = onNewProduction) {
                    Icon(Icons.Default.Factory, contentDescription = "New production")
                }
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = Color(0xFF00C853)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add material")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Header
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (state.lowStockMaterials.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFD32F2F).copy(alpha = 0.12f)
                    ) {
                        Text(
                            "${state.lowStockMaterials.size} low stock",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = Color(0xFFD32F2F)
                        )
                    }
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier  = Modifier.padding(bottom = 8.dp)
            ) {
                val statuses = listOf(null, "AVAILABLE", "LOW_STOCK", "OUT_OF_STOCK")
                items(statuses) { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick  = {
                            selectedStatus = status
                            viewModel.onStatusFilterChanged(status)
                        },
                        label    = {
                            Text(status?.replace("_", " ")?.lowercase()
                                ?.replaceFirstChar { it.uppercase() } ?: "All",
                                style = MaterialTheme.typography.labelSmall)
                        }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.materials.isEmpty() && !state.isLoading) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No materials found",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(state.materials, key = { it.id }) { material ->
                    MaterialCard(
                        material  = material,
                        onClick   = { onMaterialClick(material) },
                        onEdit    = { editingMat = material; showAddSheet = true },
                        onAdjust  = { adjustingMat = material },
                        onStatusChange = { status ->
                            viewModel.updateStatus(material.id, status)
                        }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddEditMaterialSheet(
            existing  = editingMat,
            onSave    = { request ->
                viewModel.upsertMaterial(request)
                showAddSheet = false
                editingMat   = null
            },
            onDismiss = { showAddSheet = false; editingMat = null }
        )
    }

    adjustingMat?.let { mat ->
        AdjustStockSheet(
            material  = mat,
            onConfirm = { qty, notes ->
                viewModel.adjustStock(mat.id, qty, notes)
                adjustingMat = null
            },
            onDismiss = { adjustingMat = null }
        )
    }
}

@Composable
private fun MaterialCard(
    material: MaterialEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onAdjust: () -> Unit,
    onStatusChange: (String) -> Unit,
) {
    val statusColor = when (material.status) {
        "AVAILABLE"    -> Color(0xFF388E3C)
        "LOW_STOCK"    -> Color(0xFFF57C00)
        "OUT_OF_STOCK" -> Color(0xFFD32F2F)
        else           -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(material.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold)
                    material.supplier?.let {
                        Text(it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        material.status.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = statusColor
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        com.techsultan.zenithpro.core.util.Util.formatReceiptQuantity(
                            material.quantity, 
                            UnitType.fromString(material.unit)
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    material.lowStockAlert?.let {
                        Text("Alert at ${com.techsultan.zenithpro.core.util.Util.formatReceiptQuantity(it, UnitType.fromString(material.unit))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row {
                    TextButton(onClick = onAdjust) {
                        Text("Adjust stock",
                            style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditMaterialSheet(
    existing: MaterialEntity?,
    onSave: (UpsertMaterialRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var description  by remember { mutableStateOf(existing?.description ?: "") }
    var unit by remember { mutableStateOf(existing?.unit ?: "UNITS") }
    var costPerUnit by remember { mutableStateOf(
        if (existing != null) (existing.costPerUnit / 100.0).toString() else "") }
    var lowStock by remember { mutableStateOf(existing?.lowStockAlert?.toString() ?: "") }
    var supplier by remember { mutableStateOf(existing?.supplier ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var showUnitMenu by remember { mutableStateOf(false) }

    val units = listOf("KG","GRAMS","LITRES","ML","PIECES","BAGS","CARTONS","METRES","UNITS")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (existing != null) "Edit material" else "New material",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Name *") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp))
            OutlinedTextField(value = description, onValueChange = { description = it },
                label = { Text("Description") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp))

            // Unit selector
            ExposedDropdownMenuBox(
                expanded = showUnitMenu, onExpandedChange = { showUnitMenu = it }
            ) {
                OutlinedTextField(
                    value = unit, onValueChange = {},
                    label = { Text("Unit") }, readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showUnitMenu) },
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded = showUnitMenu, onDismissRequest = { showUnitMenu = false }
                ) {
                    units.forEach { u ->
                        DropdownMenuItem(
                            text = { Text(u) },
                            onClick = { unit = u; showUnitMenu = false }
                        )
                    }
                }
            }

            OutlinedTextField(value = costPerUnit, onValueChange = { costPerUnit = it },
                label = { Text("Cost per unit") }, prefix = { Text("₦") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(10.dp))
            OutlinedTextField(value = lowStock, onValueChange = { lowStock = it },
                label = { Text("Low stock alert") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(10.dp))
            OutlinedTextField(value = supplier, onValueChange = { supplier = it },
                label = { Text("Supplier") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it },
                label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(),
                minLines = 2, shape = RoundedCornerShape(10.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(UpsertMaterialRequest(
                            id           = existing?.id,
                            name         = name.trim(),
                            description  = description.trimOrNull(),
                            unit         = unit,
                            costPerUnit  = costPerUnit.toDoubleOrNull()
                                ?.let { (it * 100).toLong() } ?: 0L,
                            lowStockAlert = lowStock.toDoubleOrNull(),
                            supplier     = supplier.trimOrNull(),
                            notes        = notes.trimOrNull()
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
            ) { Text("Save material", fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdjustStockSheet(
    material: MaterialEntity,
    onConfirm: (Double, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var amount  by remember { mutableStateOf("") }
    var notes   by remember { mutableStateOf("") }
    var isAdd   by remember { mutableStateOf(true) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Adjust stock — ${material.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Text("Current: ${com.techsultan.zenithpro.core.util.Util.formatReceiptQuantity(material.quantity, UnitType.fromString(material.unit))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(true to "Add stock", false to "Remove stock").forEach { (add, label) ->
                    OutlinedButton(
                        onClick  = { isAdd = add },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = if (isAdd == add)
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = if (add) Color(0xFF388E3C).copy(alpha = 0.12f)
                                else Color(0xFFD32F2F).copy(alpha = 0.12f)
                            )
                        else ButtonDefaults.outlinedButtonColors()
                    ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                }
            }

            OutlinedTextField(
                value         = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label         = { Text("Quantity") },
                suffix        = { Text(material.unit) },
                modifier      = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape         = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value         = notes,
                onValueChange = { notes = it },
                label         = { Text("Reason / notes") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(10.dp)
            )
            Button(
                onClick  = {
                    val qty = amount.toDoubleOrNull() ?: return@Button
                    onConfirm(if (isAdd) qty else -qty, notes.trimOrNull())
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (isAdd) Color(0xFF388E3C) else Color(0xFFD32F2F)
                )
            ) { Text("Confirm", fontWeight = FontWeight.Bold) }
        }
    }
}