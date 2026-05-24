package com.techsultan.zenithpro.core.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.techsultan.zenithpro.core.data.UserSession

@Composable
fun ZenithNavigationDrawer(
    drawerState: DrawerState,
    session: UserSession?,
    gesturesEnabled: Boolean,
    onLogout: () -> Unit,
    onCustomersClick: () -> Unit,
    onExpensesClick: () -> Unit,
    onBranchesClick: () -> Unit,
    onProductionClick: () -> Unit,
    onMaterialClick: () -> Unit,
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
                    DrawerHeader(session)
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
                    NavigationDrawerItem(
                        label = { Text("Branches") },
                        selected = false,
                        onClick = onBranchesClick,
                        icon = { Icon(Icons.Default.Store, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Production") },
                        selected = false,
                        onClick = onProductionClick,
                        icon = { Icon(Icons.Default.LocalConvenienceStore, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text("Material") },
                        selected = false,
                        onClick = onMaterialClick,
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
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
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
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
private fun DrawerHeader(session: UserSession?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        AsyncImage(
            model = session?.businessLogoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(1.dp, Color.LightGray, CircleShape),
            contentScale = ContentScale.Crop,
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = session?.businessName ?: "Zenith Pro",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = session?.branchName ?: session?.businessAddress ?: "No address set",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
