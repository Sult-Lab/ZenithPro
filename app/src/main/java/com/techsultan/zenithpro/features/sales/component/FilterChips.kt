package com.techsultan.zenithpro.features.sales.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.techsultan.zenithpro.features.sales.presentation.SalesListUiState

@Composable
fun ActiveFilterChips(
    state: SalesListUiState,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        state.filterPaymentMethod?.let { method ->
            AssistChip(
                onClick = {},
                label   = { Text(method.name, style = MaterialTheme.typography.labelSmall) }
            )
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onClear) {
            Text("Clear", style = MaterialTheme.typography.labelSmall)
        }
    }
}