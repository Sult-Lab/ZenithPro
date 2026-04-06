package com.techsultan.zenithpro.features.expenses.presentation

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.components.SummaryChip
import com.techsultan.zenithpro.core.components.SyncStatusBadge
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.expenses.data.local.CategoryBreakdown
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseEntity
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseFilter
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseSummary
import com.techsultan.zenithpro.features.expenses.viewmodel.ExpenseListViewModel
import com.techsultan.zenithpro.features.sales.formatAmount
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ExpenseListScreen(
    viewModel: ExpenseListViewModel = koinViewModel(),
    onAddExpense: () -> Unit,
    onExpenseClick: (ExpenseEntity) -> Unit,
    onBack: () -> Unit
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val filteredExpenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExpenseListViewModel.ExpenseListEvent.ShowMessage ->
                    snackbarHost.showSnackbar(event.message)
                is ExpenseListViewModel.ExpenseListEvent.ShowError ->
                    snackbarHost.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            ZenithTopAppBar(
                title = "Expenses",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    BadgedBox(badge = {
                        if (state.filter.category != null ||
                            state.filter.minAmount != null ||
                            state.filter.maxAmount != null) Badge()
                    }) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Summary cards ─────────────────────────────────────
            state.stats?.let { stats ->
                ExpenseSummaryRow(
                    summary   = stats.summary,
                    breakdown = stats.breakdown
                )
            }

            // ── Search ────────────────────────────────────────────
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search expenses...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh  = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading && filteredExpenses.isEmpty() ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }

                    filteredExpenses.isEmpty() ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier            = Modifier.padding(32.dp)
                            ) {
                                Text(
                                    "No expenses found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = onAddExpense) {
                                    Text("Record an expense")
                                }
                            }
                        }

                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Group by date
                        val grouped = filteredExpenses.groupBy { it.expenseDate }

                        grouped.forEach { (date, expensesOnDate) ->
                            stickyHeader(key = date) {
                                ExpenseDateHeader(
                                    date = LocalDate.parse(date),
                                    totalAmount = expensesOnDate.sumOf { it.amount }
                                )
                            }
                            items(expensesOnDate, key = { it.id }) { expense ->
                                ExpenseCard(
                                    expense   = expense,
                                    onClick   = { onExpenseClick(expense) },
                                    onDelete  = { viewModel.deleteExpense(expense.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Filter sheet ──────────────────────────────────────────────
    if (showFilterSheet) {
        ExpenseFilterSheet(
            currentFilter = state.filter,
            categories    = state.categories,
            onApply       = { filter ->
                viewModel.onFilterChanged(filter)
                showFilterSheet = false
            },
            onDismiss     = { showFilterSheet = false }
        )
    }
}


@Composable
private fun ExpenseSummaryRow(
    summary: ExpenseSummary,
    breakdown: List<CategoryBreakdown>,
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier              = Modifier.padding(vertical = 4.dp)
    ) {
        item {
            SummaryChip(
                label = "Total",
                value = "₦${summary.totalAmount.formatAmount()}",
                color = Color(0xFFD32F2F)
            )
        }
        item {
            SummaryChip(
                label = "Count",
                value = summary.totalCount.toString(),
                color = Color(0xFF1976D2)
            )
        }
        items(breakdown.take(4)) { cat ->
            SummaryChip(
                label = cat.category,
                value = "₦${cat.totalAmount.formatAmount()}",
                color = Color(0xFF7B1FA2)
            )
        }
    }
}

@Composable
private fun ExpenseDateHeader(date: LocalDate, totalAmount: Long) {
    val today     = LocalDate.now()
    val yesterday = today.minusDays(1)
    val label = when (date) {
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
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("₦${totalAmount.formatAmount()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: ExpenseEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Color(0xFFD32F2F).copy(alpha = 0.1f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = expense.category.first().uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = expense.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text     = expense.category,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (expense.syncStatus != Util.SyncStatus.SYNCED) {
                        SyncStatusBadge(status = expense.syncStatus)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = "₦${expense.amount.formatAmount()}",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFFD32F2F)
                )
                IconButton(
                    onClick  = { showDeleteDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text("Delete expense") },
            text    = { Text("Are you sure you want to delete \"${expense.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors  = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseFilterSheet(
    currentFilter: ExpenseFilter,
    categories: List<String>,
    onApply: (ExpenseFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var from by remember { mutableStateOf(currentFilter.from) }
    var to  by remember { mutableStateOf(currentFilter.to) }
    var category by remember { mutableStateOf(currentFilter.category) }
    var minAmount by remember { mutableStateOf(currentFilter.minAmount?.toString() ?: "") }
    var maxAmount by remember { mutableStateOf(currentFilter.maxAmount?.toString() ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Filter expenses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)

            Text("Date range", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val presets = listOf(
                    "This month" to (LocalDate.now().withDayOfMonth(1) to LocalDate.now()),
                    "Last month" to (
                            LocalDate.now().minusMonths(1).withDayOfMonth(1) to
                                    LocalDate.now().minusMonths(1).let {
                                        it.withDayOfMonth(it.lengthOfMonth())
                                    }
                            ),
                    "Last 30d"   to (LocalDate.now().minusDays(30) to LocalDate.now()),
                    "This year"  to (LocalDate.now().withDayOfYear(1) to LocalDate.now())
                )
                items(presets) { (label, range) ->
                    FilterChip(
                        selected = from == range.first && to == range.second,
                        onClick  = { from = range.first; to = range.second },
                        label    = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            if (categories.isNotEmpty()) {
                Text("Category", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick  = { category = if (category == cat) null else cat },
                            label    = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Text("Amount range", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = minAmount,
                    onValueChange = { minAmount = it.filter { c -> c.isDigit() } },
                    label         = { Text("Min") },
                    prefix        = { Text("₦") },
                    modifier      = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape         = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value         = maxAmount,
                    onValueChange = { maxAmount = it.filter { c -> c.isDigit() } },
                    label         = { Text("Max") },
                    prefix        = { Text("₦") },
                    modifier      = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape         = RoundedCornerShape(10.dp)
                )
            }

            Button(
                onClick  = {
                    onApply(ExpenseFilter(
                        from      = from,
                        to        = to,
                        category  = category,
                        minAmount = minAmount.toLongOrNull(),
                        maxAmount = maxAmount.toLongOrNull()
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(10.dp)
            ) { Text("Apply filters") }

            OutlinedButton(
                onClick  = {
                    onApply(ExpenseFilter())
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(10.dp)
            ) { Text("Clear filters") }
        }
    }
}