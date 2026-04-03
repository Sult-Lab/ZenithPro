package com.techsultan.zenithpro.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.techsultan.zenithpro.core.util.Util

@Composable
fun SyncStatusBadge(status: Util.SyncStatus) {
    val color = when (status) {
        Util.SyncStatus.PENDING -> Color.Gray
        Util.SyncStatus.DIRTY -> MaterialTheme.colorScheme.primary
        Util.SyncStatus.DELETED -> MaterialTheme.colorScheme.error
        else -> Color.Transparent
    }

    if (color != Color.Transparent) {
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}