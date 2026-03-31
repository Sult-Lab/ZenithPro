package com.techsultan.zenithpro.features.dashboard.presentation

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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.components.ZenithButton
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.features.dashboard.data.remote.ChartDataPoint
import com.techsultan.zenithpro.features.dashboard.data.remote.DashboardSummary
import com.techsultan.zenithpro.features.dashboard.data.remote.PendingDebtSummary
import com.techsultan.zenithpro.features.sales.formatAmount
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    businessId: String,
    viewModel: DashboardViewModel = koinViewModel(),
    onNewSale: () -> Unit,
    onAddProduct: () -> Unit = {},
    onViewReports: () -> Unit = {},
    onViewSales: () -> Unit,
    onViewDebts: () -> Unit,
    onViewLowStock: () -> Unit = {},
    onViewPurchaseOrders: () -> Unit = {},
) {
    LaunchedEffect(businessId) { viewModel.init(businessId) }

    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ZenithTopAppBar(
                title =  "Dashboard", //state.businessName.ifBlank { "Dashboard" },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  =  "",//state.businessName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick  = { /* notifications */ },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint               = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh    = viewModel::refresh,
            modifier     = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Date subheading + New Sale CTA ────────────────
                item {
                    DashboardHero(onNewSale = onNewSale)
                }

                // ── Today's KPI cards (2 × 2) ─────────────────────
                item {
                    TodayKpiSection(
                        summary   = state.summary,
                        isLoading = state.isLoading
                    )
                }

                // ── Profit margin progress card ────────────────────
                item {
                    ProfitMarginCard(summary = state.summary)
                }

                // ── Urgent actions ─────────────────────────────────
                item {
                    UrgentActionsSection(
                        lowStockCount       = 1/*state.lowStockCount*/,
                        pendingOrderCount   = 5/*state.pendingPurchaseOrderCount*/,
                        onViewLowStock      = onViewLowStock,
                        onViewPurchaseOrders = onViewPurchaseOrders
                    )
                }

                // ── Pending debts banner (conditional) ────────────
                if (state.pendingDebts.debtCount > 0) {
                    item {
                        PendingDebtsBanner(
                            summary   = state.pendingDebts,
                            onViewAll = onViewDebts
                        )
                    }
                }

                // ── Quick actions ──────────────────────────────────
                item {
                    QuickActionsRow(
                        onAddProduct  = onAddProduct,
                        onViewReports = onViewReports
                    )
                }

                // ── Sales trend chart ──────────────────────────────
                item {
                    SalesChartCard(
                        chartData      = state.chartData,
                        selectedDays   = state.selectedDays,
                        onPeriodChange = viewModel::onChartPeriodChanged
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

// ── Hero row ──────────────────────────────────────────────────────

@Composable
private fun DashboardHero(onNewSale: () -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text       = "Good ${greeting()},",
                style      = MaterialTheme.typography.bodyLarge,
                color      = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text  = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d yyyy")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = onNewSale,
            shape   = RoundedCornerShape(10.dp),
            colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
        ) {
            Icon(
                imageVector        = Icons.Default.Add,
                contentDescription = null,
                modifier           = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("New sale", style = MaterialTheme.typography.labelLarge)
        }

    }
}

private fun greeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> "morning"
        hour < 17 -> "afternoon"
        else      -> "evening"
    }
}

// ── KPI cards (2 × 2) ─────────────────────────────────────────────

@Composable
private fun TodayKpiSection(
    summary: DashboardSummary,
    isLoading: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text       = "Today's overview",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                modifier  = Modifier.weight(1f),
                label     = "Revenue",
                value     = "₦${summary.totalRevenue.formatAmount()}",
                icon      = Icons.AutoMirrored.Filled.TrendingUp,
                color     = Color(0xFF1976D2),
                isLoading = isLoading
            )
            KpiCard(
                modifier  = Modifier.weight(1f),
                label     = "Profit",
                value     = "₦${summary.totalProfit.formatAmount()}",
                icon      = Icons.Default.AccountBalance,
                color     = Color(0xFF388E3C),
                isLoading = isLoading
            )
        }
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                modifier  = Modifier.weight(1f),
                label     = "Orders",
                value     = summary.totalOrders.toString(),
                icon      = Icons.Default.ShoppingCart,
                color     = Color(0xFF7B1FA2),
                isLoading = isLoading
            )
            KpiCard(
                modifier  = Modifier.weight(1f),
                label     = "Outstanding",
                value     = "₦${summary.totalDebt.formatAmount()}",
                icon      = Icons.Default.Warning,
                color     = if (summary.totalDebt > 0) Color(0xFFD32F2F) else Color(0xFF388E3C),
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
        modifier  = modifier,
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border    = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = label,
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
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = color,
                        modifier           = Modifier.size(15.dp)
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
                    text       = value,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = color
                )
            }
        }
    }
}

// ── Profit margin card ─────────────────────────────────────────────

@Composable
private fun ProfitMarginCard(summary: DashboardSummary) {
    val marginColor = when {
        summary.profitMargin >= 30f -> Color(0xFF388E3C)
        summary.profitMargin >= 15f -> Color(0xFFF57C00)
        else                        -> Color(0xFFD32F2F)
    }
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border    = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Profit margin",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text       = "${String.format("%.1f", summary.profitMargin)}%",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = marginColor
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress    = { (summary.profitMargin / 100f).coerceIn(0f, 1f) },
                modifier    = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color       = marginColor,
                trackColor  = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text  = "Cost: ₦${summary.totalCost.formatAmount()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = "Revenue: ₦${summary.totalRevenue.formatAmount()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Urgent actions ─────────────────────────────────────────────────

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
            text       = "Urgent actions",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (lowStockCount > 0) {
            UrgentActionCard(
                title          = "Items low in stock",
                count          = lowStockCount.toString(),
                icon           = Icons.Default.Inventory,
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                contentColor   = MaterialTheme.colorScheme.error,
                borderColor    = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                onClick        = onViewLowStock
            )
        }

        if (pendingOrderCount > 0) {
            UrgentActionCard(
                title          = "Pending purchase orders",
                count          = pendingOrderCount.toString(),
                icon           = Icons.AutoMirrored.Filled.ReceiptLong,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor   = MaterialTheme.colorScheme.onSurface,
                borderColor    = MaterialTheme.colorScheme.outlineVariant,
                onClick        = onViewPurchaseOrders
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
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = containerColor),
        border   = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier          = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = contentColor,
                modifier           = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text       = count,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = contentColor
                )
            }
            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Pending debts banner ───────────────────────────────────────────

@Composable
private fun PendingDebtsBanner(
    summary: PendingDebtSummary,
    onViewAll: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(
            containerColor = Color(0xFFD32F2F).copy(alpha = 0.07f)
        ),
        border   = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.25f))
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFD32F2F).copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Warning,
                        contentDescription = null,
                        tint               = Color(0xFFD32F2F),
                        modifier           = Modifier.size(17.dp)
                    )
                }
                Column {
                    Text(
                        text       = "${summary.debtCount} pending debt${if (summary.debtCount > 1) "s" else ""}",
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFFD32F2F)
                    )
                    Text(
                        text  = "₦${summary.totalDebt.formatAmount()} outstanding",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD32F2F).copy(alpha = 0.75f)
                    )
                }
            }
            TextButton(onClick = onViewAll) {
                Text(
                    text  = "View all",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFD32F2F)
                )
            }
        }
    }
}

// ── Quick actions row ──────────────────────────────────────────────

@Composable
private fun QuickActionsRow(
    onAddProduct: () -> Unit,
    onViewReports: () -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ZenithButton(
            text           = "Add Product",
            icon           = Icons.Default.Add,
            onClick        = onAddProduct,
            modifier       = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor   = MaterialTheme.colorScheme.onSecondaryContainer
        )
        ZenithButton(
            text           = "View Reports",
            icon           = Icons.Default.Assessment,
            onClick        = onViewReports,
            modifier       = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor   = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// ── Sales trend chart ──────────────────────────────────────────────

@Composable
private fun SalesChartCard(
    chartData: List<ChartDataPoint>,
    selectedDays: Int,
    onPeriodChange: (Int) -> Unit,
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border    = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text       = "Sales trend",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    if (chartData.isNotEmpty()) {
                        Text(
                            text  = "₦${chartData.sumOf { it.revenue }.formatAmount()} total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Period selector pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(7, 14, 30).forEach { days ->
                        val selected = selectedDays == days
                        Surface(
                            shape    = RoundedCornerShape(6.dp),
                            color    = if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { onPeriodChange(days) }
                        ) {
                            Text(
                                text     = "${days}d",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style    = MaterialTheme.typography.labelSmall,
                                color    = if (selected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (chartData.isEmpty()) {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "No sales data for this period",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                SalesBarChart(chartData = chartData)
            }
        }
    }
}

// ── Bar chart — Canvas, no library ────────────────────────────────

@Composable
private fun SalesBarChart(chartData: List<ChartDataPoint>) {
    val maxRevenue  = chartData.maxOfOrNull { it.revenue } ?: 1L
    val barColor    = MaterialTheme.colorScheme.primary
    val profitColor = Color(0xFF388E3C)
    val labelColor  = MaterialTheme.colorScheme.onSurfaceVariant
    val formatter   = DateTimeFormatter.ofPattern("MM/dd")

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val groupWidth = size.width / chartData.size
        val barWidth   = groupWidth * 0.5f
        val maxH       = size.height * 0.78f
        val baseY      = size.height - 20.dp.toPx()

        chartData.forEachIndexed { index, point ->
            val centerX  = groupWidth * index + groupWidth / 2f

            val revenueH = if (maxRevenue > 0)
                (point.revenue.toFloat() / maxRevenue) * maxH else 0f
            drawRoundRect(
                color        = barColor.copy(alpha = 0.85f),
                topLeft      = Offset(centerX - barWidth / 2, baseY - revenueH),
                size         = Size(barWidth, revenueH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )

            val profitH = if (maxRevenue > 0)
                (point.profit.toFloat() / maxRevenue) * maxH else 0f
            drawRoundRect(
                color        = profitColor.copy(alpha = 0.75f),
                topLeft      = Offset(centerX - barWidth / 4, baseY - profitH),
                size         = Size(barWidth / 2, profitH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )

            if (chartData.size <= 14 || index % 2 == 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    LocalDate.parse(point.saleDate).format(formatter),
                    centerX,
                    size.height,
                    android.graphics.Paint().apply {
                        color     = labelColor.copy(alpha = 0.6f).toArgb()
                        textSize  = 9.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        LegendItem(color = MaterialTheme.colorScheme.primary, label = "Revenue")
        Spacer(Modifier.width(16.dp))
        LegendItem(color = Color(0xFF388E3C), label = "Profit")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

