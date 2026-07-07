package com.techsultan.zenithpro.features.analytics.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techsultan.zenithpro.features.expenses.data.local.CategoryBreakdown
import com.techsultan.zenithpro.features.sales.formatAmount

@Composable
fun ExpenseCategoryBar(category: CategoryBreakdown, maxAmount: Long) {
    val fraction = if (maxAmount > 0) category.totalAmount.toFloat() / maxAmount else 0f
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier  = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category.category,
                style = MaterialTheme.typography.bodySmall)
            Text(
                text = "₦${category.totalAmount.formatAmount()}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress   = { fraction },
            modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Color(0xFFD32F2F),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}