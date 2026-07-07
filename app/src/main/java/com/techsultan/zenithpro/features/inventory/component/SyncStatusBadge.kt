package com.techsultan.zenithpro.features.inventory.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.techsultan.zenithpro.core.util.Util

@Composable
private fun SyncStatusBadge(status: Util.SyncStatus) {
    val (label, color) = when (status) {
        Util.SyncStatus.PENDING -> "Saving..." to Color(0xFFFFA000)
        Util.SyncStatus.DIRTY   -> "Unsynced" to Color(0xFF1976D2)
        Util.SyncStatus.DELETED -> "Deleting..." to Color(0xFFD32F2F)
        Util.SyncStatus.SYNCED  -> return
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.padding(start = 6.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}