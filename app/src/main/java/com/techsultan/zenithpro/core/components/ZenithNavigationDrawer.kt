package com.techsultan.zenithpro.core.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ZenithNavigationDrawer(
    drawerState: DrawerState,
    gesturesEnabled: Boolean,
    onLogout: () -> Unit,
    onCustomersClick: () -> Unit,
    onExpensesClick: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(Modifier.height(12.dp))
                    DrawerHeader()
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    NavigationDrawerItem(
                        label = { Text("Customers") },
                        selected = false,
                        onClick = onCustomersClick,
                        icon = { Icon(Icons.Default.People, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Expenses") },
                        selected = false,
                        onClick = onExpensesClick,
                        icon = { Icon(Icons.Default.Payments, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        label = { Text("Help & Support") },
                        selected = false,
                        onClick = { /* TODO */ },
                        icon = { Icon(Icons.Default.Help, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Logout") },
                        selected = false,
                        onClick = onLogout,
                        icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        },
        content = content
    )
}

@Composable
private fun DrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ZenithPro",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Manage your business with ease",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
