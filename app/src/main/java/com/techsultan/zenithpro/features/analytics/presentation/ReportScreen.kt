package com.techsultan.zenithpro.features.analytics.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.features.analytics.component.CustomerRankCard
import com.techsultan.zenithpro.features.analytics.component.ExpenseCategoryBar
import com.techsultan.zenithpro.features.analytics.component.ReportComparisonCard
import com.techsultan.zenithpro.features.analytics.component.ReportCustomerCard
import com.techsultan.zenithpro.features.analytics.component.ReportSalesSummaryCard
import com.techsultan.zenithpro.features.analytics.component.SalesChartCard
import com.techsultan.zenithpro.features.analytics.data.ReportPeriod
import com.techsultan.zenithpro.features.analytics.data.TopProductRow
import com.techsultan.zenithpro.features.analytics.data.remote.StaffPerformance
import com.techsultan.zenithpro.features.sales.formatAmount
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = koinViewModel(),
    onMenuClick: () -> Unit,
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ZenithTopAppBar(
                title = "Business Reports",
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    if (state.canFilterBranch || state.canFilterStaff) {
                        BadgedBox(badge = { if (state.hasActiveFilters) Badge() }) {
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (state.isLoading && state.data == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@PullToRefreshBox
            }

            val data = state.data ?: return@PullToRefreshBox

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Period selector ─────────────────────────────────
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ReportPeriod.entries.filter { it != ReportPeriod.CUSTOM }) { period ->
                            FilterChip(
                                selected = state.selectedPeriod == period,
                                onClick  = { viewModel.onPeriodChanged(period) },
                                label    = { Text(period.label(),
                                    style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // ── Active filter chips ──────────────────────────────
                if (state.hasActiveFilters) {
                    item {
                        ActiveReportFilterChips(state = state, onClear = viewModel::clearFilters)
                    }
                }

                // ── Sales summary ────────────────────────────────────
                item { ReportSalesSummaryCard(summary = data.salesSummary) }

                // ── Profit vs expenses ───────────────────────────────
                item {
                    ReportComparisonCard(
                        totalProfit   = data.salesSummary.totalProfit,
                        totalExpenses = data.expenseSummary.totalAmount
                    )
                }

                // ── Sales trend chart ────────────────────────────────
                item {
                    SalesChartCard(
                        chartData      = data.chartData,
                        selectedDays   = data.chartData.size,
                        onPeriodChange = {}
                    )
                }

                // ── Top products ─────────────────────────────────────
                if (data.topProducts.isNotEmpty()) {
                    item {
                        Text("Top selling products",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                    }
                    items(data.topProducts, key = { it.productName }) { product ->
                        TopProductRow(
                            product   = product,
                            maxRevenue = data.topProducts.first().revenue
                        )
                    }
                }

                // ── Staff performance — admin/manager only ───────────
                if (state.canFilterStaff && data.staffPerformance.isNotEmpty()) {
                    item {
                        Text("Staff performance",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                    }
                    items(data.staffPerformance, key = { it.staffId }) { staff ->
                        StaffPerformanceRow(
                            staff      = staff,
                            maxRevenue = data.staffPerformance.first().revenue
                        )
                    }
                }

                // ── Expense breakdown ────────────────────────────────
                if (data.expenseBreakdown.isNotEmpty()) {
                    item {
                        Text("Expenses by category",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                    }
                    items(data.expenseBreakdown, key = { it.category }) { cat ->
                        ExpenseCategoryBar(
                            category  = cat,
                            maxAmount = data.expenseBreakdown.first().totalAmount
                        )
                    }
                }

                // ── Customer overview ────────────────────────────────
                item { ReportCustomerCard(stats = data.customerStats) }

                // ── Top spenders ─────────────────────────────────────
                if (data.topSpenders.isNotEmpty()) {
                    item {
                        Text(
                            text = "Top spenders",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                    }
                    items(data.topSpenders, key = { "report_${it.id}" }) { customer ->
                        CustomerRankCard(
                            customer   = customer,
                            valueLabel = "₦${customer.totalSpent.formatAmount()}",
                            valueColor = Color(0xFF1976D2)
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ReportFilterSheet(
            state     = state,
            onApply   = { branchId, staffId ->
                viewModel.onBranchFilterChanged(branchId)
                viewModel.onStaffFilterChanged(staffId)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportFilterSheet(
    state: ReportsUiState,
    onApply: (String?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedBranch by remember { mutableStateOf(state.filterBranchId) }
    val staffOptions = state.data?.staffPerformance ?: emptyList()
    var selectedStaff by remember { mutableStateOf(state.filterStaffId) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Filter reports",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)

            if (state.canFilterBranch && state.branches.isNotEmpty()) {
                Text("Branch", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedBranch == null,
                            onClick  = { selectedBranch = null },
                            label    = { Text("All branches",
                                style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    items(
                        items = state.branches
                    ) { branch ->
                        FilterChip(
                            selected = selectedBranch == branch.id,
                            onClick  = {
                                selectedBranch = if (selectedBranch == branch.id) null else branch.id
                            },
                            label    = { Text(branch.name,
                                style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            if (state.canFilterStaff && staffOptions.isNotEmpty()) {
                Text("Staff", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedStaff == null,
                            onClick  = { selectedStaff = null },
                            label    = { Text("All staff",
                                style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    items(staffOptions) { staff ->
                        FilterChip(
                            selected = selectedStaff == staff.staffId,
                            onClick  = {
                                selectedStaff = if (selectedStaff == staff.staffId) null
                                else staff.staffId
                            },
                            label    = { Text(staff.staffName,
                                style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Button(
                onClick  = { onApply(selectedBranch, selectedStaff) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(10.dp)
            ) { Text("Apply filters") }

            OutlinedButton(
                onClick  = { onApply(null, null) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(10.dp)
            ) { Text("Clear filters") }
        }
    }
}

@Composable
private fun ActiveReportFilterChips(
    state: ReportsUiState,
    onClear: () -> Unit,
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.filterBranchId?.let { id ->
            val name = state.branches.firstOrNull { it.id == id }?.name ?: "Branch"
            AssistChip(onClick = {}, label = { Text(name, style = MaterialTheme.typography.labelSmall) })
        }
        state.filterStaffId?.let { id ->
            val name = state.data?.staffPerformance
                ?.firstOrNull { it.staffId == id }?.staffName ?: "Staff"
            AssistChip(onClick = {}, label = { Text(name, style = MaterialTheme.typography.labelSmall) })
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onClear) { Text("Clear", style = MaterialTheme.typography.labelSmall) }
    }
}

// ── Sub-components ───────────────────────────────────────────────────

@Composable
private fun TopProductRow(product: TopProductRow, maxRevenue: Long) {
    val fraction = if (maxRevenue > 0) product.revenue.toFloat() / maxRevenue else 0f
    Card(shape = RoundedCornerShape(10.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(product.productName,
                    style    = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                Text("₦${product.revenue.formatAmount()}",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF388E3C))
            }
            Text("${product.unitsSold} units sold",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress   = { fraction },
                modifier   = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                color      = Color(0xFF388E3C),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun StaffPerformanceRow(staff: StaffPerformance, maxRevenue: Long) {
    val fraction = if (maxRevenue > 0) staff.revenue.toFloat() / maxRevenue else 0f
    Card(shape = RoundedCornerShape(10.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF7B1FA2).copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(staff.staffName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(staff.staffName,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Text("${staff.orderCount} sales",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress   = { fraction },
                    modifier   = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color      = Color(0xFF7B1FA2),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(Modifier.width(10.dp))
            Text("₦${staff.revenue.formatAmount()}",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold)
        }
    }
}
