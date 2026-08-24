package com.techsultan.zenithpro.features.production.presentation

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.techsultan.zenithpro.features.production.data.local.ProductionOrderEntity
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionScreen(
    viewModel: ProductionViewModel = koinViewModel(),
    onBack: () -> Unit,
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost  = remember { SnackbarHostState() }
    var showNewOrder  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProductionViewModel.ProductionEvent.OrderCreated ->
                    snackbarHost.showSnackbar("Production order created")
                is ProductionViewModel.ProductionEvent.OrderCompleted ->
                    snackbarHost.showSnackbar("Production completed — stock updated")
                is ProductionViewModel.ProductionEvent.ShowError ->
                    snackbarHost.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Production orders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { showNewOrder = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New order")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.orders.isEmpty() && !state.isLoading) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No production orders",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(state.orders, key = { it.id }) { order ->
                ProductionOrderCard(
                    order       = order,
                    isCompleting = state.isCompleting,
                    onStart     = { viewModel.startOrder(order.id) },
                    onComplete  = { viewModel.completeOrder(order.id) },
                    onCancel    = { viewModel.cancelOrder(order.id) }
                )
            }
        }
    }

    if (showNewOrder) {
        NewProductionOrderSheet(
            onSave    = { variantId, qty, notes ->
                viewModel.createOrder(
                    variantId = variantId,
                    quantity = qty,
                    notes = notes)
                showNewOrder = false
            },
            onDismiss = { showNewOrder = false }
        )
    }
}

@Composable
private fun ProductionOrderCard(
    order: ProductionOrderEntity,
    isCompleting: Boolean,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
) {
    val statusColor = when (order.status) {
        "DRAFT"       -> Color(0xFF757575)
        "IN_PROGRESS" -> Color(0xFF1976D2)
        "COMPLETED"   -> Color(0xFF388E3C)
        "CANCELLED"   -> Color(0xFFD32F2F)
        else          -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Order ${order.id.take(8)}...",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        order.status.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = statusColor
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text("Qty: ${com.techsultan.zenithpro.core.util.Util.formatQuantity(order.quantity, UnitType.UNIT)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            order.notes?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                LocalDateTime.parse(order.createdAt.take(19))
                    .format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (order.status in listOf("DRAFT", "IN_PROGRESS")) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (order.status == "DRAFT") {
                        OutlinedButton(
                            onClick  = onStart,
                            shape    = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("Start") }
                    }
                    Button(
                        onClick  = onComplete,
                        enabled  = !isCompleting,
                        shape    = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                    ) {
                        if (isCompleting) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(16.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else Text("Complete")
                    }
                    OutlinedButton(
                        onClick  = onCancel,
                        shape    = RoundedCornerShape(8.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Cancel") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewProductionOrderSheet(
    onSave: (String, Double, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var variantId by remember { mutableStateOf("") }
    var quantity  by remember { mutableStateOf("") }
    var notes     by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("New production order",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = variantId, onValueChange = { variantId = it },
                label = { Text("Variant ID") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value = quantity, onValueChange = { quantity = it },
                label = { Text("Quantity to produce") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            Button(
                onClick  = {
                    val qty = quantity.toDoubleOrNull() ?: return@Button
                    if (variantId.isNotBlank() && qty > 0) {
                        onSave(variantId, qty, notes.trimOrNull())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
            ) { Text("Create order", fontWeight = FontWeight.Bold) }
        }
    }
}