package com.techsultan.zenithpro.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector


data class BottomNavItem(
    val title: String,
    val selectedIcon: ImageVector,
)

val TOP_LEVEL_DESTINATIONS = mapOf(
    Route.Home.Dashboard to BottomNavItem(
        title = "Dashboard",
        selectedIcon = Icons.Default.Dashboard
    ),
    Route.Home.Inventory to BottomNavItem(
        title = "Inventory",
        selectedIcon = Icons.Default.Inventory
    ),
    Route.Home.Sales to BottomNavItem(
        title = "Sales",
        selectedIcon = Icons.Default.Receipt
    ),
    Route.Home.Reports to BottomNavItem(
        title = "Reports",
        selectedIcon = Icons.Default.BarChart
    ),
    Route.Home.Settings to BottomNavItem(
        title = "Settings",
        selectedIcon = Icons.Default.Settings
    )
)