package com.techsultan.zenithpro.core.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.techsultan.zenithpro.features.product.presentation.AddProductScreen
import com.techsultan.zenithpro.features.dashboard.presentation.DashboardScreen
import com.techsultan.zenithpro.features.product.presentation.InventoryScreen
import com.techsultan.zenithpro.features.sales.presentation.NewSaleScreen
import com.techsultan.zenithpro.features.analytics.presentation.ReportScreen
import com.techsultan.zenithpro.features.sales.presentation.SalesScreen
import com.techsultan.zenithpro.features.settings.presentation.SettingsScreen
import com.techsultan.zenithpro.core.components.ZenithBottomNavigation

@Composable
fun MainNavGraph(){
    val navigationState = rememberNavigationState(
        startRoute = Route.Home.Dashboard,
        topLevelDestinations = TOP_LEVEL_DESTINATIONS.keys
    )
    val navigator = remember { Navigator(navigationState) }
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            ZenithBottomNavigation(
                currentRoute = navigationState.topLevelRoute,
                onNavigate = { navigator.navigate(it) }
            )
        }
    ) { paddingValues ->

        NavDisplay(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            onBack = navigator::goBack,
            entries = navigationState.toEntries(
                entryProvider {

                    entry<Route.Home.Dashboard> {
                        DashboardScreen(
                            businessId = "a6d7b373-52c6-4ae3-84ff-b4bc06d2a46d",
                            onNewSale = { navigator.navigate(Route.Home.NewSale) },
                            onAddProduct = { navigator.navigate(Route.Home.AddProduct) },
                            onViewReports = {},
                            onViewSales = {},
                            onViewDebts = {},
                            onViewLowStock = {},
                            onViewPurchaseOrders = {}
                        )
                    }
                    entry<Route.Home.Inventory> {
                        InventoryScreen(
                            onAddProductClick = { navigator.navigate(Route.Home.AddProduct) },
                            businessId = "a6d7b373-52c6-4ae3-84ff-b4bc06d2a46d",
                            onProductClick = {}
                        )
                    }
                    entry<Route.Home.Sales> {
                        SalesScreen(
                            businessId = "a6d7b373-52c6-4ae3-84ff-b4bc06d2a46d",
                            onSaleClick = {},
                            onNewSale = {}
                        )
                    }
                    entry<Route.Home.Reports> {
                        ReportScreen()
                    }
                    entry<Route.Home.Settings> {
                        SettingsScreen()
                    }
                    entry<Route.Home.NewSale> {
                        NewSaleScreen(
                            businessId = "a6d7b373-52c6-4ae3-84ff-b4bc06d2a46d",
                            onBack = { navigator.goBack() }
                        )
                    }
                    entry<Route.Home.AddProduct> {
                        AddProductScreen(
                            navigateBack = { navigator.goBack() }
                        )
                    }
                }
            )
        )
    }
}