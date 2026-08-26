package com.techsultan.zenithpro.features.dashboard.presentation

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.techsultan.zenithpro.features.analytics.component.SalesChartCard
import com.techsultan.zenithpro.core.components.ZenithButton
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.features.dashboard.data.remote.ChartDataPoint
import com.techsultan.zenithpro.features.dashboard.data.remote.DashboardSummary
import com.techsultan.zenithpro.features.dashboard.data.remote.PendingDebtSummary
import com.techsultan.zenithpro.features.dashboard.presentation.components.BranchSelectorBar
import com.techsultan.zenithpro.features.dashboard.presentation.components.BranchSelectorBottomSheet
import com.techsultan.zenithpro.features.sales.formatAmount
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel(),
    onNewSale: () -> Unit,
    onAddProduct: () -> Unit = {},
    onViewReports: () -> Unit = {},
    onViewSales: () -> Unit,
    onViewDebts: () -> Unit,
    onViewLowStock: () -> Unit = {},
    onViewPurchaseOrders: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    var showBranchSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ZenithTopAppBar(
                title = "Dashboard",
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { /* notifications */ },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (state.isAdmin && state.branches.size > 1) {
                    item {
                        BranchSelectorBar(
                            activeBranchName = state.activeBranchName,
                            onClick = { showBranchSheet = true }
                        )
                    }
                }

                item {
                    DashboardHero(onNewSale = onNewSale)
                }

                item {
                    TodayKpiSection(
                        summary = state.summary,
                        isLoading = state.isLoading,
                        showProfit = state.showProfit
                    )
                }

                if (state.showProfit) {
                    item {
                        ProfitMarginCard(summary = state.summary)
                    }
                }

                if (state.showInventoryAlerts) {
                    item {
                        UrgentActionsSection(
                            lowStockCount = state.lowStockCount,
                            pendingOrderCount = state.pendingPurchaseOrderCount,
                            onViewLowStock = onViewLowStock,
                            onViewPurchaseOrders = onViewPurchaseOrders
                        )
                    }
                }

                if (state.pendingDebts.debtCount > 0 && state.showInventoryAlerts) {
                    item {
                        PendingDebtsBanner(
                            summary = state.pendingDebts,
                            onViewAll = onViewDebts
                        )
                    }
                }

                item {
                    QuickActionsRow(
                        onAddProduct = onAddProduct,
                        onViewReports = onViewReports,
                        showAddProduct = state.showInventoryAlerts,
                        showReports = state.showInventoryAlerts
                    )
                }

                item {
                    SalesChartCard(
                        chartData = state.chartData,
                        selectedDays = state.selectedDays,
                        onPeriodChange = viewModel::onChartPeriodChanged
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        if (showBranchSheet) {
            BranchSelectorBottomSheet(
                branches = state.branches,
                activeBranchId = state.activeBranchId,
                onBranchSelected = viewModel::onBranchSelected,
                onDismiss = { showBranchSheet = false }
            )
        }
    }
}

@Composable
private fun DashboardHero(onNewSale: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            val hour = java.time.LocalTime.now().hour
            val greeting = when {
                hour < 12 -> "morning"
                hour < 17 -> "afternoon"
                else -> "evening"
            }
            Text(
                text = "Good $greeting,",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d yyyy")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = onNewSale,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("New sale", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TodayKpiSection(
    summary: DashboardSummary,
    isLoading: Boolean,
    showProfit: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Today's overview",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                modifier = Modifier.weight(1f),
                label = "Revenue",
                value = "₦${summary.totalRevenue.formatAmount()}",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = Color(0xFF1976D2),
                isLoading = isLoading
            )
            if (showProfit) {
                KpiCard(
                    modifier = Modifier.weight(1f),
                    label = "Profit",
                    value = "₦${summary.totalProfit.formatAmount()}",
                    icon = Icons.Default.AccountBalance,
                    color = Color(0xFF388E3C),
                    isLoading = isLoading
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                modifier = Modifier.weight(1f),
                label = "Orders",
                value = summary.totalOrders.toString(),
                icon = Icons.Default.ShoppingCart,
                color = Color(0xFF7B1FA2),
                isLoading = isLoading
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                label = "Outstanding",
                value = "₦${summary.totalDebt.formatAmount()}",
                icon = Icons.Default.Warning,
                color = if (summary.totalDebt > 0) Color(0xFFD32F2F) else Color(0xFF388E3C),
                isLoading = isLoading
            )
        }
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    isLoading: Boolean,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .height(22.dp)
                        .width(72.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        )
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun ProfitMarginCard(summary: DashboardSummary) {
    val marginColor = when {
        summary.profitMargin >= 30f -> Color(0xFF388E3C)
        summary.profitMargin >= 15f -> Color(0xFFF57C00)
        else -> Color(0xFFD32F2F)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profit margin",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${String.format("%.1f", summary.profitMargin)}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = marginColor
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (summary.profitMargin / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = marginColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cost: ₦${summary.totalCost.formatAmount()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Revenue: ₦${summary.totalRevenue.formatAmount()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UrgentActionsSection(
    lowStockCount: Int,
    pendingOrderCount: Int,
    onViewLowStock: () -> Unit,
    onViewPurchaseOrders: () -> Unit,
) {
    if (lowStockCount == 0 && pendingOrderCount == 0) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Urgent actions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (lowStockCount > 0) {
            UrgentActionCard(
                title = "Items low in stock",
                count = lowStockCount.toString(),
                icon = Icons.Default.Inventory,
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.error,
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                onClick = onViewLowStock
            )
        }

        if (pendingOrderCount > 0) {
            UrgentActionCard(
                title = "Pending purchase orders",
                count = pendingOrderCount.toString(),
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                onClick = onViewPurchaseOrders
            )
        }
    }
}

@Composable
private fun UrgentActionCard(
    title: String,
    count: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = count,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PendingDebtsBanner(
    summary: PendingDebtSummary,
    onViewAll: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD32F2F).copy(alpha = 0.07f)
        ),
        border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFD32F2F).copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(17.dp)
                    )
                }
                Column {
                    Text(
                        text = "${summary.debtCount} pending debt${if (summary.debtCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD32F2F)
                    )
                    Text(
                        text = "₦${summary.totalDebt.formatAmount()} outstanding",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD32F2F).copy(alpha = 0.75f)
                    )
                }
            }
            TextButton(onClick = onViewAll) {
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFD32F2F)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onAddProduct: () -> Unit,
    onViewReports: () -> Unit,
    showAddProduct: Boolean = true,
    showReports: Boolean = true
) {
    if (!showAddProduct && !showReports) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showAddProduct) {
            ZenithButton(
                text = "Add Product",
                icon = Icons.Default.Add,
                onClick = onAddProduct,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        if (showReports) {
            ZenithButton(
                text = "View Reports",
                icon = Icons.Default.Assessment,
                onClick = onViewReports,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
