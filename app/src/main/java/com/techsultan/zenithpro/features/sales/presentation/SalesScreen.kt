package com.techsultan.zenithpro.features.sales.presentation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.R
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.util.Util.formatAsTime
import com.techsultan.zenithpro.core.util.Util.toUtcLocalDate
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.SaleStatus
import com.techsultan.zenithpro.features.sales.component.EmptySalesState
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import com.techsultan.zenithpro.features.sales.formatAmount
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: SalesListViewModel = koinViewModel(),
    onSaleClick: (String) -> Unit,
    onNewSale: () -> Unit,
    onMenuClick: () -> Unit = {},
    onNombaClick: () -> Unit = {},
    onReceiptPreview: (ReceiptData) -> Unit = {}
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val filteredSales by viewModel.filteredSales.collectAsStateWithLifecycle()
    val summary by viewModel.summaryStats.collectAsStateWithLifecycle()
    val snackbarHost  = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SalesListViewModel.SalesListEvent.ShowError ->
                    snackbarHost.showSnackbar(event.message)
                is SalesListViewModel.SalesListEvent.PrintSuccess ->
                    snackbarHost.showSnackbar("Receipt reprinted successfully")
                is SalesListViewModel.SalesListEvent.ReceiptReady -> {
                    onReceiptPreview(event.receipt)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            ZenithTopAppBar(
                title = "Sales",
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNombaClick() }) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Nomba",
                            tint = Color.Unspecified,
                            modifier = Modifier
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick  = onNewSale,
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

                            item {
                                SalesSearchBar(
                                    searchQuery    = state.searchQuery,
                                    onSearchChange = viewModel::onSearchQueryChanged
                                )
                            }

                            item {
                                SalesFilterRow(
                                    state = state,
                                    onDatePresetSelected = { from, to ->
                                        viewModel.onDateRangeChanged(from, to)
                                    },
                                    onPaymentMethodSelected = viewModel::onPaymentMethodFilterChanged,
                                    onStaffSelected = viewModel::onStaffFilterChanged,
                                    onBranchSelected = viewModel::onBranchFilterChanged,
                                    onClearFilters = viewModel::clearFilters
                                )
                            }

                            item {
                                SalesSummaryHeroCard(
                                    summary  = summary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }

                            item {
                                SalesSecondaryStats(
                                    summary  = summary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            item {
                                Row(
                                    modifier              = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Transactions",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (filteredSales.isNotEmpty()) {
                                        Text(
                                            text = "${filteredSales.size} records",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (filteredSales.isEmpty() && !state.isLoading) {
                                item { EmptySalesState() }
                            }

                            val grouped = filteredSales.groupBy { saleWithItems ->
                                saleWithItems.sale.soldAt
                                    .toUtcLocalDate() ?: LocalDate.MIN
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
                                        branches = state.availableBranches,
                                        onClick = { onSaleClick(saleWithItems.sale.id) },
                                        onReprint = { viewModel.reprintReceipt(saleWithItems) },
                                        modifier  = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        onShare = { viewModel.onShareSale(saleWithItems) }
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
        placeholder = { Text("Search by product, SKU, amount…") },
        leadingIcon = {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalesFilterRow(
    state: SalesListUiState,
    onDatePresetSelected: (LocalDate, LocalDate) -> Unit,
    onPaymentMethodSelected: (PaymentMethod?) -> Unit,
    onStaffSelected: (String?) -> Unit,
    onBranchSelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
) {
    var showDateMenu    by remember { mutableStateOf(false) }
    var showPaymentMenu by remember { mutableStateOf(false) }
    var showStaffMenu   by remember { mutableStateOf(false) }
    var showBranchMenu  by remember { mutableStateOf(false) }

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

    val hasAnyFilter = hasDateFilter || state.filterPaymentMethod != null ||
            state.filterStaffId != null || state.filterBranchId != null

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
            .padding(vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier              = Modifier.weight(1f),
            contentPadding      = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            item {
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
            }

            item {
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
            }

            if (!state.isStaff) {
                item {
                    Box {
                        FilterChip(
                            selected = state.filterStaffId != null,
                            onClick = { showStaffMenu = true },
                            label = {
                                Text(
                                    text = if (state.filterStaffId != null) "Staff active" else "Staff",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                        DropdownMenu(
                            expanded = showStaffMenu,
                            onDismissRequest = { showStaffMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All staff", style = MaterialTheme.typography.bodySmall) },
                                onClick = {
                                    onStaffSelected(null)
                                    showStaffMenu = false
                                },
                                leadingIcon = if (state.filterStaffId == null) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                            HorizontalDivider()
                            state.availableStaff.forEach { staff ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            staff.firstName,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    onClick = {
                                        onStaffSelected(staff.id)
                                        showStaffMenu = false
                                    },
                                    leadingIcon = if (state.filterStaffId == staff.id) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            if (state.isAdmin || (state.isManager && state.filterBranchId == null && state.availableBranches.size > 1)) {
                item {
                    Box {
                        FilterChip(
                            selected     = state.filterBranchId != null,
                            onClick      = { showBranchMenu = true },
                            label        = {
                                val branchName = state.availableBranches.find { it.id == state.filterBranchId }?.name
                                Text(
                                    text  = branchName ?: "Branch",
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
                            expanded         = showBranchMenu,
                            onDismissRequest = { showBranchMenu = false }
                        ) {
                            DropdownMenuItem(
                                text    = { Text("All branches", style = MaterialTheme.typography.bodySmall) },
                                onClick = {
                                    onBranchSelected(null)
                                    showBranchMenu = false
                                },
                                leadingIcon = if (state.filterBranchId == null) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                            HorizontalDivider()
                            state.availableBranches.forEach { branch ->
                                DropdownMenuItem(
                                    text    = { Text(branch.name, style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        onBranchSelected(branch.id)
                                        showBranchMenu = false
                                    },
                                    leadingIcon = if (state.filterBranchId == branch.id) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Clear all (only visible when any filter is active) ────
        if (hasAnyFilter) {
            TextButton(
                onClick = onClearFilters,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
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


@Composable
private fun SalesSummaryHeroCard(
    summary: SalesSummary,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                text = "₦${summary.totalRevenue.formatAmount()}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
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
            modifier  = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color
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

@Composable
private fun DateGroupHeader(date: LocalDate, totalSales: Long) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val label = when (date) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
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


@Composable
private fun SaleCard(
    saleWithItems: SaleWithItems,
    branches: List<BranchEntity>,
    onClick: () -> Unit,
    onReprint: () -> Unit,
    modifier: Modifier = Modifier,
    onShare: () -> Unit
) {
    val sale = saleWithItems.sale
    val branchName = remember(sale.branchId, branches) {
        branches.firstOrNull { it.id == sale.branchId }?.name
    }
    var showMenu by remember { mutableStateOf(false) }

    val (typeIcon, iconBg, iconTint) = when (sale.paymentMethod) {
        PaymentMethod.CASH     -> Triple(Icons.Default.Money,       Color(0xFFE8F5E9), Color(0xFF2E7D32))
        PaymentMethod.TRANSFER -> Triple(Icons.Default.CreditCard,  Color(0xFFE3F2FD), Color(0xFF1976D2))
        PaymentMethod.POS     -> Triple(Icons.Default.PointOfSale, Color(0xFFF3E5F5), Color(0xFF7B1FA2))
        PaymentMethod.SPLIT    -> Triple(Icons.AutoMirrored.Filled.CallSplit,   Color(0xFFFFF3E0), Color(0xFFF57C00))
        PaymentMethod.DEBT     -> Triple(Icons.Default.Warning,     Color(0xFFFFEBEE), Color(0xFFC62828))
        PaymentMethod.USSD -> Triple(Icons.Default.Dialpad,     Color(0xFFFFEBEE), Color(0xFF10E4EA))
        PaymentMethod.CARD -> Triple(Icons.Default.Dialpad,     Color(0xFFFFEBEE), Color(0xFF830A2D))
    }

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors =CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth().padding(end = 32.dp)
                ) {
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
                        val itemsText = saleWithItems.items
                            .take(2)
                            .joinToString(", ") { it.productName }
                            .let {
                                if (saleWithItems.items.size > 2)
                                    "$it +${saleWithItems.items.size - 2} more"
                                else it
                            }
                        
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = itemsText.ifBlank { "Sale Transaction" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = remember(sale.soldAt) { sale.soldAt.formatAsTime() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "₦${sale.totalAmount.formatAmount()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(2.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(iconTint)
                            )

                            Text(
                                text = sale.paymentMethod.name
                                    .lowercase()
                                    .replaceFirstChar { it.uppercase() },
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
                    branchName?.let { name ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Store, null,
                                    tint     = Color(0xFF1976D2),
                                    modifier = Modifier.size(10.dp))
                                Text(name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1976D2))
                            }
                        }
                    }
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
                            text = "₦${sale.debtAmount.formatAmount()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }

            // More Icon positioned at TopEnd
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 4.dp)) {
                IconButton(
                    onClick = { showMenu = true }
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More"
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Reprint Receipt",
                                style = MaterialTheme.typography.bodyMedium
                            )
                               },
                        onClick = {
                            showMenu = false
                            onReprint()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Print,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Share Receipt",
                                style = MaterialTheme.typography.bodyMedium
                            )
                               },
                        onClick = {
                            showMenu = false
                            onShare()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null
                            )
                        }
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
        onSaleClick = {},
        onNewSale = {}
    )
}
