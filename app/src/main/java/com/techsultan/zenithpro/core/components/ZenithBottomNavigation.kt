package com.techsultan.zenithpro.core.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.techsultan.zenithpro.core.navigation.Route
import com.techsultan.zenithpro.core.navigation.TOP_LEVEL_DESTINATIONS


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
