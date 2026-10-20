package com.techsultan.zenithpro.features.inventory.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants
import java.util.Locale
import kotlin.math.min

data class PieChartSlice(
    val label: String,
    val value: Double,
    val color: Color
)

private val SliceColors = listOf(
    Color(0xFF1976D2), // Blue
    Color(0xFF388E3C), // Green
    Color(0xFFF57C00), // Orange
    Color(0xFF7B1FA2), // Purple
    Color(0xFF00897B), // Teal
    Color(0xFFD32F2F), // Red
    Color(0xFFE91E63), // Pink
    Color(0xFF00ACC1), // Cyan
    Color(0xFFFBC02D)  // Amber
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryPieChartBottomSheet(
    products: List<ProductWithVariants>,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Category, 1 = Stock Status

    val categorySlices = remember(products) {
        val grouped = products.groupBy { it.product.category?.takeIf { c -> c.isNotBlank() } ?: "Uncategorized" }
        grouped.entries.mapIndexed { index, (category, productList) ->
            val totalQty = productList.sumOf { p ->
                p.variants.sumOf { v -> v.stock.sumOf { s -> s.quantity } }
            }
            PieChartSlice(
                label = category,
                value = totalQty,
                color = SliceColors[index % SliceColors.size]
            )
        }.sortedByDescending { it.value }
    }

    val statusSlices = remember(products) {
        var inStockQty = 0.0
        var lowStockQty = 0.0
        var outOfStockQty = 0.0

        products.forEach { p ->
            val totalStock = p.variants.sumOf { v -> v.stock.sumOf { s -> s.quantity } }
            val isLowStock = p.variants.any { v ->
                v.stock.any { s -> s.quantity <= (s.lowStockAlert ?: 5) }
            }

            when {
                totalStock <= 0 -> outOfStockQty += 1.0
                isLowStock -> lowStockQty += totalStock
                else -> inStockQty += totalStock
            }
        }

        listOf(
            PieChartSlice("In Stock", inStockQty, Color(0xFF388E3C)),
            PieChartSlice("Low Stock", lowStockQty, Color(0xFFF57C00)),
            PieChartSlice("Out of Stock", outOfStockQty, Color(0xFFD32F2F))
        ).filter { it.value > 0 }
    }

    val currentSlices = if (selectedTab == 0) categorySlices else statusSlices
    val totalValue = currentSlices.sumOf { it.value }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Inventory Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Tab Switcher
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Category", "Status").forEachIndexed { index, label ->
                        val selected = selectedTab == index
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable { selectedTab = index }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Donut Pie Chart Canvas
            if (totalValue > 0) {
                PieChartCanvas(
                    slices = currentSlices,
                    modifier = Modifier.size(200.dp),
                    centerValue = String.format(Locale.US, "%,.0f", totalValue),
                    centerLabel = if (selectedTab == 0) "Total Units" else "Units / Items"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Legend List
                PieLegendGrid(
                    slices = currentSlices,
                    total = totalValue
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No inventory items available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PieChartCanvas(
    slices: List<PieChartSlice>,
    modifier: Modifier = Modifier,
    centerValue: String = "",
    centerLabel: String = ""
) {
    val total = slices.sumOf { it.value }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 32.dp.toPx()
            val diameter = min(size.width, size.height) - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)

            var startAngle = -90f
            slices.forEach { slice ->
                val sweepAngle = ((slice.value / total) * 360f).toFloat()
                if (sweepAngle > 0f) {
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - 1.5f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }
        }

        if (centerLabel.isNotEmpty() || centerValue.isNotEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = centerValue,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PieLegendGrid(
    slices: List<PieChartSlice>,
    total: Double
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        slices.forEach { slice ->
            val percentage = if (total > 0) (slice.value / total) * 100.0 else 0.0
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(slice.color, CircleShape)
                        )
                        Text(
                            text = slice.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Locale.US, "%,.0f units", slice.value),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = slice.color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f%%", percentage),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = slice.color,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
