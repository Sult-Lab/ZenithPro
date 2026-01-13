package com.techsultan.zenithpro.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techsultan.zenithpro.presentation.components.ZenithBottomNavigation
import com.techsultan.zenithpro.presentation.components.ZenithTopAppBar

@Composable
fun ReportScreen() {
    Scaffold(
        topBar = {
            ZenithTopAppBar(
                title = "Business Reports",
                navigationIcon = {
                    // No back button as requested
                },
                actions = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                FilterChipItem(text = "Last 7 Days", selected = true)
                FilterChipItem(text = "Today", selected = false)
                FilterChipItem(text = "This Month", selected = false)
                FilterChipItem(text = "Custom", selected = false)
            }

            // Revenue and Profit Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryReportCard(
                    title = "Total Revenue",
                    amount = "₦1.25M",
                    modifier = Modifier.weight(1f)
                )
                SummaryReportCard(
                    title = "Net Profit",
                    amount = "₦450K",
                    modifier = Modifier.weight(1f)
                )
            }

            // Items Sold Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Items Sold",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "342",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Sales Performance Card (Graph)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sales Performance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₦1.25M",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Last 7 Days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "➚ +15.3%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Simple Graph Placeholder using Canvas
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)) {
                        SalesGraph()
                    }
                }
            }

            // Inventory Movement Header
            Text(
                text = "Inventory Movement",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Low Stock Items
            InventoryMovementCard(
                title = "Low Stock Items",
                subtitle = "5 items are running low",
                icon = Icons.Default.Warning,
                iconTint = MaterialTheme.colorScheme.error,
                iconBg = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )

            // Fastest Moving Items
            InventoryMovementCard(
                title = "Fastest Moving Items",
                subtitle = "Peak Milk, Indomie Noodles...",
                icon = Icons.Default.LocalShipping,
                iconTint = Color(0xFF2E7D32),
                iconBg = Color(0xFFE8F5E9)
            )

            // Profit Analysis Header
            Text(
                text = "Profit Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Average Profit Margin
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Average Profit Margin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "36%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Cost of Goods Sold
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cost of Goods Sold (COGS)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₦800,000",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryReportCard(
    title: String,
    amount: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun InventoryMovementCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
fun SalesGraph() {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val points = listOf(0.6f, 0.4f, 0.5f, 0.2f, 0.3f, 0.7f, 0.9f, 0.4f)
        val stepX = width / (points.size - 1)
        
        val path = Path()
        path.moveTo(0f, height * points[0])
        
        for (i in 1 until points.size) {
            val prevX = (i - 1) * stepX
            val prevY = height * points[i - 1]
            val currX = i * stepX
            val currY = height * points[i]
            
            // Cubic bezier for smooth curve
            val controlPoint1X = prevX + (currX - prevX) / 2
            val controlPoint1Y = prevY
            val controlPoint2X = prevX + (currX - prevX) / 2
            val controlPoint2Y = currY
            
            path.cubicTo(controlPoint1X, controlPoint1Y, controlPoint2X, controlPoint2Y, currX, currY)
        }
        
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw labels
        // Note: Simple text drawing in Canvas requires TextMeasurer or native canvas, 
        // usually simpler to overlay composables in a Box, but here we just draw the line.
    }
    
    // Day Labels overlay
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
fun PreviewReport() {
    ReportScreen()
}
