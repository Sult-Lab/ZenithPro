package com.techsultan.zenithpro.features.production.presentation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.core.domain.domain.UnitType
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.production.data.local.ProductionOrderEntity
import com.techsultan.zenithpro.features.production.presentation.components.NewProductionOrderSheet
import com.techsultan.zenithpro.features.production.presentation.components.UpdateStatusSheet
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

private data class ProductionFilterOption(
    val label: String,
    val statusKey: String?,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionScreen(
    viewModel: ProductionViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recipeMaterials by viewModel.recipeMaterials.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var showNewOrderSheet by remember { mutableStateOf(false) }
    var selectedOrderForUpdate by remember { mutableStateOf<ProductionOrderEntity?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchField by remember { mutableStateOf(false) }

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
            ZenithTopAppBar(
                title = "Production Orders",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchField = !showSearchField }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                    IconButton(onClick = { /* Filter menu */ }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewOrderSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New production order",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar (if toggled)
            if (showSearchField || searchQuery.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search production orders...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            // Filter Chips Horizontal Row
            val filterOptions = listOf(
                ProductionFilterOption("All", null, Icons.Default.CheckCircle),
                ProductionFilterOption("Active", "IN_PROGRESS", Icons.Default.Timer),
                ProductionFilterOption("On Hold", "ON_HOLD", Icons.Default.PauseCircle),
                ProductionFilterOption("Completed", "COMPLETED", Icons.Default.TaskAlt),
                ProductionFilterOption("Draft", "DRAFT", Icons.Default.EditNote),
                ProductionFilterOption("Canceled", "CANCELLED", Icons.Default.Block)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                items(filterOptions) { option ->
                    val isSelected = selectedStatusFilter == option.statusKey
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedStatusFilter = option.statusKey
                            viewModel.onStatusFilterChanged(option.statusKey)
                        },
                        label = {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                            selectedLabelColor = MaterialTheme.colorScheme.surface,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.surface,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            // Summary KPI Cards Row (Active Production & Est Value)
            val activeOrders = remember(state.orders) {
                state.orders.filter { it.status == "IN_PROGRESS" || it.status == "DRAFT" || it.status == "ON_HOLD" }
            }
            val totalActiveUnits = remember(activeOrders) {
                activeOrders.sumOf { it.quantity }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active Production Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder(enabled = true),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "ACTIVE PRODUCTION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${Util.formatQuantity(totalActiveUnits, UnitType.UNIT)} Units",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Est. Value Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder(enabled = true),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "EST. VALUE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₦ ${String.format(Locale.US, "%,.0f", totalActiveUnits * 1500)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Filtered Orders List grouped by Date
            val displayedOrders = remember(state.orders, searchQuery) {
                if (searchQuery.isBlank()) {
                    state.orders
                } else {
                    state.orders.filter { order ->
                        order.id.contains(searchQuery, ignoreCase = true) ||
                                (order.notes?.contains(searchQuery, ignoreCase = true) == true) ||
                                (order.status.contains(searchQuery, ignoreCase = true))
                    }
                }
            }

            val groupedOrders = remember(displayedOrders) {
                groupOrdersByDate(displayedOrders)
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (displayedOrders.isEmpty() && !state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No orders match '$searchQuery'" else "No production orders",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                groupedOrders.forEach { (dateGroup, ordersInGroup) ->
                    item {
                        Text(
                            text = dateGroup,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(ordersInGroup, key = { it.id }) { order ->
                        val matchedProduct = remember(state.products, order.variantId) {
                            state.products.firstOrNull { p ->
                                p.variants.any { v -> v.variant.id == order.variantId }
                            }
                        }
                        val matchedVariant = remember(matchedProduct, order.variantId) {
                            matchedProduct?.variants?.firstOrNull { v -> v.variant.id == order.variantId }
                        }

                        ProductionOrderItemCard(
                            order = order,
                            productName = matchedProduct?.product?.name ?: "Batch #${order.id.take(8).uppercase()}",
                            sku = matchedVariant?.variant?.sku ?: order.variantId.take(8).uppercase(),
                            isCompleting = state.isCompleting,
                            onStart = { viewModel.startOrder(order.id) },
                            onComplete = { selectedOrderForUpdate = order },
                            onCancel = { viewModel.cancelOrder(order.id) },
                            onOpenUpdateStatus = { selectedOrderForUpdate = order }
                        )
                    }
                }
            }
        }
    }

    if (showNewOrderSheet) {
        NewProductionOrderSheet(
            products = state.products,
            materials = state.materials,
            onSave = { variantId, qty, notes, materials ->
                viewModel.createOrder(variantId, qty, notes)
                showNewOrderSheet = false
            },
            onDismiss = {
                showNewOrderSheet = false
                viewModel.clearRecipeSelection()
                        },
            recipeMaterials = recipeMaterials,
            onProductSelected = { variantId -> viewModel.onProductSelectedForRecipe(variantId) }
        )
    }

    selectedOrderForUpdate?.let { order ->
        val matchedProduct = remember(state.products, order.variantId) {
            state.products.firstOrNull { p ->
                p.variants.any { v -> v.variant.id == order.variantId }
            }
        }

        UpdateStatusSheet(
            order = order,
            productName = matchedProduct?.product?.name ?: "Production Item",
            unitType = matchedProduct?.product?.unitType,
            onSaveStatus = { newStatus, actualYield, remarks ->
                when (newStatus) {
                    "IN_PROGRESS" -> viewModel.startOrder(order.id)
                    "COMPLETED" -> {
                        val yield = actualYield ?: order.quantity
                        viewModel.completeOrder(
                            orderId = order.id,
                            actualQuantity = yield,
                            remarks = remarks
                        )
                    }
                    "CANCELLED" -> viewModel.cancelOrder(order.id)
                }
                selectedOrderForUpdate = null
            },
            onDismiss = { selectedOrderForUpdate = null }
        )
    }
}

@Composable
private fun ProductionOrderItemCard(
    order: ProductionOrderEntity,
    productName: String,
    sku: String,
    isCompleting: Boolean,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onOpenUpdateStatus: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenUpdateStatus),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Product Thumbnail Container
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
                            imageVector = Icons.Default.Factory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Order Info Column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Batch #$sku",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Status Badge
                    val (badgeContainer, badgeContentColor, badgeIcon, badgeText) = when (order.status) {
                        "IN_PROGRESS" -> Quadruple(Color(0xFFE3F2FD), Color(0xFF1976D2), Icons.Default.PlayCircle, "In Progress")
                        "ON_HOLD" -> Quadruple(Color(0xFFFFF3E0), Color(0xFFE65100), Icons.Default.PauseCircle, "On Hold")
                        "COMPLETED" -> Quadruple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Default.TaskAlt, "Completed")
                        "CANCELLED" -> Quadruple(Color(0xFFFFEBEE), Color(0xFFC62828), Icons.Default.Block, "Canceled")
                        else -> Quadruple(Color(0xFFFFF8E1), Color(0xFFF57C00), Icons.Default.Timer, "Draft / Pending")
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = badgeContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = badgeIcon,
                                contentDescription = null,
                                tint = badgeContentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeContentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${Util.formatQuantity(order.quantity, UnitType.UNIT)} Units",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Options Menu Dots
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Update status") },
                            onClick = {
                                showMenu = false
                                onOpenUpdateStatus()
                            }
                        )
                        if (order.status in listOf("DRAFT", "PENDING", "ON_HOLD")) {
                            DropdownMenuItem(
                                text = { Text("Start production") },
                                onClick = {
                                    showMenu = false
                                    onStart()
                                }
                            )
                        }
                        if (order.status in listOf("DRAFT", "PENDING", "IN_PROGRESS", "ON_HOLD")) {
                            DropdownMenuItem(
                                text = { Text("Complete production") },
                                onClick = {
                                    showMenu = false
                                    onComplete()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cancel order", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onCancel()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer row with date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (order.status) {
                            "IN_PROGRESS" -> "Started ${order.startedAt?.let { Util.formatRelativeTime(it) } ?: Util.formatRelativeTime(order.createdAt)}"
                            "COMPLETED" -> "Finished ${order.completedAt?.let { Util.formatRelativeTime(it) } ?: Util.formatRelativeTime(order.updatedAt)}"
                            else -> "Created ${Util.formatRelativeTime(order.createdAt)}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isCompleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

private fun groupOrdersByDate(orders: List<ProductionOrderEntity>): Map<String, List<ProductionOrderEntity>> {
    val now = LocalDate.now()
    return orders.groupBy { order ->
        val date = runCatching {
            Instant.parse(order.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }.getOrElse {
            runCatching {
                LocalDateTime.parse(order.createdAt.take(19)).toLocalDate()
            }.getOrElse { now }
        }
        when {
            date == now -> "TODAY"
            date == now.minusDays(1) -> "YESTERDAY"
            ChronoUnit.DAYS.between(date, now) < 7 -> "EARLIER THIS WEEK"
            else -> "PREVIOUS"
        }
    }
}
