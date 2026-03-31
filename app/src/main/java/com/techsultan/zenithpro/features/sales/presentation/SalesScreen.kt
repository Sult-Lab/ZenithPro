package com.techsultan.zenithpro.features.sales.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.core.theme.blueColor
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.SaleStatus
import com.techsultan.zenithpro.features.sales.component.EmptySalesState
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import com.techsultan.zenithpro.features.sales.formatAmount
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SalesScreen(
    businessId: String,
    viewModel: SalesListViewModel = koinViewModel(),
    onSaleClick: (String) -> Unit,
    onNewSale: () -> Unit,
) {
    LaunchedEffect(businessId) { viewModel.init(businessId) }

    val state         by viewModel.state.collectAsStateWithLifecycle()
    val filteredSales by viewModel.filteredSales.collectAsStateWithLifecycle()
    val summary       by viewModel.summaryStats.collectAsStateWithLifecycle()
    val snackbarHost  = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SalesListViewModel.SalesListEvent.ShowError ->
                    snackbarHost.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            ZenithTopAppBar(
                title = "Sales",
                actions = {
                    IconButton(onClick = { /* export / report */ }) {
                        Icon(
                            imageVector        = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint               = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = onNewSale,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "New sale")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && filteredSales.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh    = viewModel::refresh,
                        modifier     = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        LazyColumn(
                            modifier            = Modifier.fillMaxSize(),
                            contentPadding      = PaddingValues(bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // ── Search bar ────────────────────────────────
                            item {
                                SalesSearchBar(
                                    searchQuery    = state.searchQuery,
                                    onSearchChange = viewModel::onSearchQueryChanged
                                )
                            }

                            // ── Inline filter chips ───────────────────────
                            item {
                                SalesFilterRow(
                                    state                      = state,
                                    onDatePresetSelected       = { from, to ->
                                        viewModel.onDateRangeChanged(from, to)
                                    },
                                    onPaymentMethodSelected    = viewModel::onPaymentMethodFilterChanged,
                                    onStaffSelected            = viewModel::onStaffFilterChanged,
                                    onClearFilters             = viewModel::clearFilters
                                )
                            }

                            // ── Summary hero card ─────────────────────────
                            item {
                                SalesSummaryHeroCard(
                                    summary  = summary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }

                            // ── Secondary summary chips ───────────────────
                            item {
                                SalesSecondaryStats(
                                    summary  = summary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            // ── Section header ────────────────────────────
                            item {
                                Row(
                                    modifier              = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text       = "Transactions",
                                        style      = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (filteredSales.isNotEmpty()) {
                                        Text(
                                            text  = "${filteredSales.size} records",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // ── Empty state ───────────────────────────────
                            if (filteredSales.isEmpty() && !state.isLoading) {
                                item { EmptySalesState() }
                            }

                            // ── Date-grouped sale cards ───────────────────
                            val grouped = filteredSales.groupBy { saleWithItems ->
                                saleWithItems.sale.soldAt
                                    .let { Instant.parse(it) }
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            }

                            grouped.forEach { (date, salesOnDate) ->
                                stickyHeader(key = "header-$date") {
                                    DateGroupHeader(
                                        date       = date,
                                        totalSales = salesOnDate.sumOf { it.sale.totalAmount }
                                    )
                                }
                                items(
                                    items = salesOnDate,
                                    key   = { it.sale.id }
                                ) { saleWithItems ->
                                    SaleCard(
                                        saleWithItems = saleWithItems,
                                        onClick       = { onSaleClick(saleWithItems.sale.id) },
                                        modifier      = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Search bar ────────────────────────────────────────────────────

@Composable
private fun SalesSearchBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
) {
    OutlinedTextField(
        value         = searchQuery,
        onValueChange = onSearchChange,
        modifier      = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder   = { Text("Search by product, SKU, amount…") },
        leadingIcon   = {
            Icon(
                imageVector        = Icons.Default.Search,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        singleLine = true,
        shape      = RoundedCornerShape(12.dp),
        colors     = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor   = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor      = MaterialTheme.colorScheme.primary
        )
    )
}

// ── Inline filter row (replaces bottom sheet) ─────────────────────

@Composable
private fun SalesFilterRow(
    state: SalesListUiState,
    onDatePresetSelected: (LocalDate, LocalDate) -> Unit,
    onPaymentMethodSelected: (PaymentMethod?) -> Unit,
    onStaffSelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
) {
    var showDateMenu    by remember { mutableStateOf(false) }
    var showPaymentMenu by remember { mutableStateOf(false) }
    var showStaffMenu   by remember { mutableStateOf(false) }

    val datePresets = listOf(
        "Today"      to (LocalDate.now() to LocalDate.now()),
        "Yesterday"  to (LocalDate.now().minusDays(1) to LocalDate.now().minusDays(1)),
        "This week"  to (LocalDate.now().with(DayOfWeek.MONDAY) to LocalDate.now()),
        "This month" to (LocalDate.now().withDayOfMonth(1) to LocalDate.now()),
        "Last 30 days" to (LocalDate.now().minusDays(30) to LocalDate.now())
    )

    val hasDateFilter = run {
        val defaultFrom = LocalDate.now().minusDays(30)
        val defaultTo   = LocalDate.now()
        state.filterFrom != defaultFrom || state.filterTo != defaultTo
    }

    val dateLabel = when {
        state.filterFrom == LocalDate.now() && state.filterTo == LocalDate.now() -> "Today"
        state.filterFrom == LocalDate.now().minusDays(1)
                && state.filterTo == LocalDate.now().minusDays(1)               -> "Yesterday"
        state.filterFrom == LocalDate.now().with(DayOfWeek.MONDAY)
                && state.filterTo == LocalDate.now()                             -> "This week"
        state.filterFrom == LocalDate.now().withDayOfMonth(1)
                && state.filterTo == LocalDate.now()                             -> "This month"
        hasDateFilter                                                             -> "Custom date"
        else                                                                     -> "Date"
    }

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {

        // ── Date filter chip ──────────────────────────────────────
        Box {
            FilterChip(
                selected      = hasDateFilter,
                onClick       = { showDateMenu = true },
                label         = { Text(dateLabel, style = MaterialTheme.typography.labelSmall) },
                trailingIcon  = {
                    Icon(
                        imageVector        = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier           = Modifier.size(14.dp)
                    )
                },
                shape = RoundedCornerShape(20.dp)
            )
            DropdownMenu(
                expanded        = showDateMenu,
                onDismissRequest = { showDateMenu = false }
            ) {
                datePresets.forEach { (label, range) ->
                    DropdownMenuItem(
                        text    = { Text(label, style = MaterialTheme.typography.bodySmall) },
                        onClick = {
                            onDatePresetSelected(range.first, range.second)
                            showDateMenu = false
                        },
                        leadingIcon = if (
                            state.filterFrom == range.first && state.filterTo == range.second
                        ) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }

        // ── Payment method filter chip ────────────────────────────
        Box {
            FilterChip(
                selected     = state.filterPaymentMethod != null,
                onClick      = { showPaymentMenu = true },
                label        = {
                    Text(
                        text  = state.filterPaymentMethod
                            ?.name?.lowercase()?.replaceFirstChar { it.uppercase() }
                            ?: "Payment",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector        = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier           = Modifier.size(14.dp)
                    )
                },
                shape = RoundedCornerShape(20.dp)
            )
            DropdownMenu(
                expanded         = showPaymentMenu,
                onDismissRequest = { showPaymentMenu = false }
            ) {
                DropdownMenuItem(
                    text    = { Text("All methods", style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        onPaymentMethodSelected(null)
                        showPaymentMenu = false
                    },
                    leadingIcon = if (state.filterPaymentMethod == null) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
                HorizontalDivider()
                PaymentMethod.entries.forEach { method ->
                    DropdownMenuItem(
                        text    = {
                            Text(
                                text  = method.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        onClick = {
                            onPaymentMethodSelected(method)
                            showPaymentMenu = false
                        },
                        leadingIcon = if (state.filterPaymentMethod == method) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }

        // ── Staff filter chip ─────────────────────────────────────
        Box {
            FilterChip(
                selected     = state.filterStaffId != null,
                onClick      = { showStaffMenu = true },
                label        = {
                    Text(
                        text  = if (state.filterStaffId != null) "Staff active" else "Staff",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector        = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier           = Modifier.size(14.dp)
                    )
                },
                shape = RoundedCornerShape(20.dp)
            )
            DropdownMenu(
                expanded         = showStaffMenu,
                onDismissRequest = { showStaffMenu = false }
            ) {
                DropdownMenuItem(
                    text    = { Text("All staff", style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        onStaffSelected(null)
                        showStaffMenu = false
                    },
                    leadingIcon = if (state.filterStaffId == null) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
                HorizontalDivider()
//                state.availableStaff.forEach { staff ->
//                    DropdownMenuItem(
//                        text    = { Text(staff.name, style = MaterialTheme.typography.bodySmall) },
//                        onClick = {
//                            onStaffSelected(staff.id)
//                            showStaffMenu = false
//                        },
//                        leadingIcon = if (state.filterStaffId == staff.id) {
//                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
//                        } else null
//                    )
//                }
            }
        }

        // ── Clear all (only visible when any filter is active) ────
        if (hasDateFilter || state.filterPaymentMethod != null || state.filterStaffId != null) {
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick      = onClearFilters,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text  = "Clear",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ── Summary hero card ─────────────────────────────────────────────

@Composable
private fun SalesSummaryHeroCard(
    summary: SalesSummary,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = "TOTAL REVENUE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f),
                    letterSpacing = 0.8.sp
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text     = "${summary.totalOrders} orders",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text       = "₦${summary.totalRevenue.formatAmount()}",
                style      = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeroStatItem(
                    label = "Collected",
                    value = "₦${summary.totalCollected.formatAmount()}",
                    color = Color.White
                )
                HeroStatItem(
                    label = "Outstanding",
                    value = "₦${summary.totalDebt.formatAmount()}",
                    color = if (summary.totalDebt > 0) Color(0xFFFFCDD2) else Color.White
                )
                HeroStatItem(
                    label = "Avg. order",
                    value = "₦${summary.averageOrderValue.formatAmount()}",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun HeroStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = color
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f)
        )
    }
}

// ── Secondary stat chips ──────────────────────────────────────────

@Composable
private fun SalesSecondaryStats(
    summary: SalesSummary,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier              = modifier
    ) {
        item {
            SecondaryStatChip(
                icon  = Icons.Default.Receipt,
                label = "Cash",
                value = "₦${summary.cashRevenue.formatAmount()}",
                color = Color(0xFF388E3C)
            )
        }
        item {
            SecondaryStatChip(
                icon  = Icons.Default.CreditCard,
                label = "Transfer",
                value = "₦${summary.transferRevenue.formatAmount()}",
                color = Color(0xFF1976D2)
            )
        }
        item {
            SecondaryStatChip(
                icon  = Icons.Default.PointOfSale,
                label = "Card / POS",
                value = "₦${summary.cardRevenue.formatAmount()}",
                color = Color(0xFF7B1FA2)
            )
        }
    }
}

@Composable
private fun SecondaryStatChip(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
) {
    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = color,
                modifier           = Modifier.size(14.dp)
            )
            Column {
                Text(
                    text       = value,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = color
                )
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ── Date group header ─────────────────────────────────────────────

@Composable
private fun DateGroupHeader(date: LocalDate, totalSales: Long) {
    val today     = LocalDate.now()
    val yesterday = today.minusDays(1)
    val label     = when (date) {
        today     -> "Today"
        yesterday -> "Yesterday"
        else      -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = label,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text  = "₦${totalSales.formatAmount()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Sale card ─────────────────────────────────────────────────────

@Composable
private fun SaleCard(
    saleWithItems: SaleWithItems,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sale = saleWithItems.sale

    val (typeIcon, iconBg, iconTint) = when (sale.paymentMethod) {
        PaymentMethod.CASH     -> Triple(Icons.Default.Money,       Color(0xFFE8F5E9), Color(0xFF2E7D32))
        PaymentMethod.TRANSFER -> Triple(Icons.Default.CreditCard,  Color(0xFFE3F2FD), Color(0xFF1976D2))
        PaymentMethod.POS     -> Triple(Icons.Default.PointOfSale, Color(0xFFF3E5F5), Color(0xFF7B1FA2))
        PaymentMethod.SPLIT    -> Triple(Icons.AutoMirrored.Filled.CallSplit,   Color(0xFFFFF3E0), Color(0xFFF57C00))
        PaymentMethod.DEBT     -> Triple(Icons.Default.Warning,     Color(0xFFFFEBEE), Color(0xFFC62828))
        PaymentMethod.USSD -> Triple(Icons.Default.Dialpad,     Color(0xFFFFEBEE), Color(0xFF10E4EA))
    }

    Card(
        modifier  = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border    = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth()
            ) {
                // Payment-type icon
                Box(
                    modifier        = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = typeIcon,
                        contentDescription = null,
                        tint               = iconTint,
                        modifier           = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Item names
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = saleWithItems.items
                            .take(2)
                            .joinToString(", ") { it.productName }
                            .let {
                                if (saleWithItems.items.size > 2)
                                    "$it +${saleWithItems.items.size - 2} more"
                                else it
                            },
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = Instant.parse(sale.soldAt)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("h:mm a")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Amount + status
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text       = "₦${sale.totalAmount.formatAmount()}",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(iconTint)
                        )
                        Text(
                            text  = sale.paymentMethod.name
                                .lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Status badge (non-completed only)
            if (sale.status != SaleStatus.COMPLETED) {
                Spacer(Modifier.height(8.dp))
                SaleStatusBadge(status = sale.status)
            }

            // Debt row
            if (sale.debtAmount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFD32F2F).copy(alpha = 0.07f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = "Outstanding debt",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD32F2F)
                    )
                    Text(
                        text       = "₦${sale.debtAmount.formatAmount()}",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}

@Composable
private fun SaleStatusBadge(status: SaleStatus) {
    if (status == SaleStatus.COMPLETED) return
    val (label, color) = when (status) {
        SaleStatus.PARTIAL   -> "Partial"   to Color(0xFFF57C00)
        SaleStatus.REFUNDED  -> "Refunded"  to Color(0xFF1976D2)
        SaleStatus.CANCELLED -> "Cancelled" to Color(0xFF757575)
        else                 -> return
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text       = label,
            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style      = MaterialTheme.typography.labelSmall,
            color      = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview
@Composable
fun PreviewSales() {
    SalesScreen(
        businessId = "",
        onSaleClick = {},
        onNewSale = {}
    )
}
