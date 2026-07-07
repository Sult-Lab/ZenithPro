package com.techsultan.zenithpro.features.analytics.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techsultan.zenithpro.features.dashboard.data.remote.ChartDataPoint
import com.techsultan.zenithpro.features.sales.formatAmount
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SalesChartCard(
    chartData: List<ChartDataPoint>,
    selectedDays: Int,
    onPeriodChange: (Int) -> Unit,
) {
    Card(
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Sales trend",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(7, 14, 30).forEach { days ->
                        val selected = selectedDays == days
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { onPeriodChange(days) }
                        ) {
                            Text(
                                text = "${days}d",
                                modifier = Modifier.padding(
                                    horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected)
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
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "No sales data",
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

@Composable
private fun SalesBarChart(chartData: List<ChartDataPoint>) {
    val maxRevenue = chartData.maxOfOrNull { it.revenue } ?: 1L
    val barColor = MaterialTheme.colorScheme.primary
    val profitColor = Color(0xFF388E3C)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val formatter = DateTimeFormatter.ofPattern("MM/dd")

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val groupWidth = size.width / chartData.size
        val barWidth   = groupWidth * 0.5f
        val maxH       = size.height * 0.8f

        chartData.forEachIndexed { index, point ->
            val centerX = groupWidth * index + groupWidth / 2f

            val revenueH = if (maxRevenue > 0)
                (point.revenue.toFloat() / maxRevenue) * maxH else 0f
            drawRoundRect(
                color = barColor.copy(alpha = 0.85f),
                topLeft = Offset(centerX - barWidth / 2, size.height - maxH + (maxH - revenueH) - 20.dp.toPx()),
                size = Size(barWidth, revenueH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )

            val profitH = if (maxRevenue > 0)
                (point.profit.toFloat() / maxRevenue) * maxH else 0f
            drawRoundRect(
                color        = profitColor.copy(alpha = 0.7f),
                topLeft      = Offset(centerX - barWidth / 4, size.height - maxH + (maxH - profitH) - 20.dp.toPx()),
                size         = Size(barWidth / 2, profitH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )

            if (chartData.size <= 14 || index % 2 == 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    LocalDate.parse(point.saleDate).format(formatter),
                    centerX,
                    size.height - 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = labelColor.copy(alpha = 0.7f).toArgb()
                        textSize  = 9.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        LegendItem(color = MaterialTheme.colorScheme.primary, label = "Revenue")
        Spacer(Modifier.width(16.dp))
        LegendItem(color = Color(0xFF388E3C), label = "Profit")
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
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}