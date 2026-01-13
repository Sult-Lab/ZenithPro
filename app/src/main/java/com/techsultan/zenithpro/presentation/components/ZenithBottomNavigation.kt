package com.techsultan.zenithpro.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.techsultan.zenithpro.navigation.Route
import com.techsultan.zenithpro.navigation.TOP_LEVEL_DESTINATIONS


@Composable
fun ZenithBottomNavigation(
    currentRoute: Route,
    onNavigate: (Route) -> Unit
) {

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        TOP_LEVEL_DESTINATIONS.forEach { (topLevelDestination, data) ->
            NavigationBarItem(
                icon = { Icon(imageVector = data.selectedIcon, contentDescription = data.title) },
                label = { Text(text = data.title) },
                selected = currentRoute == topLevelDestination,
                onClick = { onNavigate(topLevelDestination) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}
