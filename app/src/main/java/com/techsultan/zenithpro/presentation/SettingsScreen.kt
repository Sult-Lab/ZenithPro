package com.techsultan.zenithpro.presentation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techsultan.zenithpro.presentation.components.ZenithBottomNavigation
import com.techsultan.zenithpro.presentation.components.ZenithTopAppBar

@Composable
fun SettingsScreen() {
    Scaffold(
        topBar = {
            ZenithTopAppBar(
                title = "Settings",
                navigationIcon = {
                    // No navigation icon for top level settings
                },
                actions = { }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Admin Section
            SettingsSection(
                title = "ADMIN",
                items = listOf(
                    SettingsItemData("Staff Management", Icons.Default.Groups),
                    SettingsItemData("Business Information", Icons.Default.Store)
                )
            )

            // General Section
            SettingsSection(
                title = "GENERAL",
                items = listOf(
                    SettingsItemData("Account Settings", Icons.Default.Person),
                    SettingsItemData("Notifications", Icons.Default.Notifications),
                    SettingsItemData("About App", Icons.Default.Info)
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = { /*TODO*/ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Logout",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

data class SettingsItemData(
    val title: String,
    val icon: ImageVector
)

@Composable
fun SettingsSection(
    title: String,
    items: List<SettingsItemData>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White), // White background as per image
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    SettingsItem(item = item)
                    if (index < items.size - 1) {
                        androidx.compose.material3.Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItem(item: SettingsItemData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Preview
@Composable
fun PreviewSettings() {
    SettingsScreen()
}
