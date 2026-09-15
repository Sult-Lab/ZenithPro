package com.techsultan.zenithpro.features.analytics.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techsultan.zenithpro.features.analytics.data.ReportSalesSummary
import com.techsultan.zenithpro.features.sales.formatAmount

@Composable
fun ReportSalesSummaryCard(
    summary: ReportSalesSummary,
    showProfit: Boolean = true
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sales overview",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val stats = mutableListOf(
                    Triple("Revenue",  "₦${summary.totalRevenue.formatAmount()}", Color(0xFF1976D2)),
                    Triple("Orders", summary.totalOrders.toString(),             Color(0xFF7B1FA2)),
                )

                if (showProfit) {
                    stats.add(1, Triple("Profit", "₦${summary.totalProfit.formatAmount()}",  Color(0xFF388E3C)))
                    stats.add(Triple("Margin", "${String.format("%.1f", summary.profitMargin)}%",
                        if (summary.profitMargin >= 20f) Color(0xFF388E3C) else Color(0xFFF57C00)))
                }

                stats.forEach { (label, value, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color      = color)
                        Text(label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}