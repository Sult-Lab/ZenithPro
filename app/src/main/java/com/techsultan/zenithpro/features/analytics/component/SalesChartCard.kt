package com.techsultan.zenithpro.features.analytics.component

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(chartData) {
        if (chartData.isNotEmpty()) {
            modelProducer.runTransaction {
                columnModel {
                    series(chartData.map { it.revenue })
                }
                lineModel {
                    series(chartData.map { it.profit })
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sales trend",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (chartData.isNotEmpty()) {
                        Text(
                            text = "₦${chartData.sumOf { it.revenue }.formatAmount()} total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sales data for this period",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val bottomAxisValueFormatter = CartesianValueFormatter { _, value, _ ->
                    chartData.getOrNull(value.toInt())?.saleDate?.let {
                        LocalDate.parse(it).format(DateTimeFormatter.ofPattern("MM/dd"))
                    } ?: value.toInt().toString()
                }

                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(
                            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                rememberLineComponent(
                                    fill = Fill(MaterialTheme.colorScheme.primary),
                                    thickness = 8.dp,
                                    shape = RoundedCornerShape(2.dp)
                                )
                            )
                        ),
                        rememberLineCartesianLayer(
                            lineProvider = LineCartesianLayer.LineProvider.series(
                                LineCartesianLayer.rememberLine(
                                    fill = LineCartesianLayer.LineFill.single(Fill(Color(0xFF388E3C)))
                                )
                            )
                        ),
                        startAxis = VerticalAxis.rememberStart(
                            line = null,
                            label = null,
                            tick = null,
                            guideline = rememberLineComponent(
                                fill = Fill(MaterialTheme.colorScheme.outlineVariant),
                                thickness = 1.dp
                            )
                        ),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            valueFormatter = bottomAxisValueFormatter,
                            itemPlacer = HorizontalAxis.ItemPlacer.aligned()
                        ),
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    LegendItem(color = MaterialTheme.colorScheme.primary, label = "Revenue")
                    Spacer(Modifier.width(16.dp))
                    LegendItem(color = Color(0xFF388E3C), label = "Profit")
                }
            }
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
