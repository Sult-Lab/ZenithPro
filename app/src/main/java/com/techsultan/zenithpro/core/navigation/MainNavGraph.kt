package com.techsultan.zenithpro.core.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.techsultan.zenithpro.core.components.ZenithBottomNavigation
import com.techsultan.zenithpro.core.components.ZenithNavigationDrawer
import com.techsultan.zenithpro.features.analytics.presentation.ReportScreen
import com.techsultan.zenithpro.features.customer.presentation.CustomerListScreen
import com.techsultan.zenithpro.features.dashboard.presentation.DashboardScreen
import com.techsultan.zenithpro.features.expenses.presentation.ExpenseListScreen
import com.techsultan.zenithpro.features.product.presentation.AddProductScreen
import com.techsultan.zenithpro.features.product.presentation.InventoryScreen
import com.techsultan.zenithpro.features.sales.presentation.NewSaleScreen
import com.techsultan.zenithpro.features.sales.presentation.SalesScreen
import com.techsultan.zenithpro.features.settings.presentation.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun MainNavGraph() {
    val navigationState = rememberNavigationState(
        startRoute = Route.Home.Dashboard,
        topLevelDestinations = TOP_LEVEL_DESTINATIONS.keys + setOf(Route.Home.Customers, Route.Home.Expenses)
    )
    val navigator = remember { Navigator(navigationState) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Gesture and Bottom Bar visibility logic
    val isBottomBarDestination = navigationState.topLevelRoute in TOP_LEVEL_DESTINATIONS.keys
    val currentStack = navigationState.backStacks[navigationState.topLevelRoute]
    val isAtRootOfStack = currentStack?.size == 1
    val gesturesEnabled = isBottomBarDestination && isAtRootOfStack

    ZenithNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        onLogout = { /* TODO: Implement logout */ },
        onCustomersClick = {
            scope.launch {
                drawerState.close()
                navigator.navigate(Route.Home.Customers)
            }
        },
        onExpensesClick = {
            scope.launch {
                drawerState.close()
                navigator.navigate(Route.Home.Expenses)
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            bottomBar = {
                if (isBottomBarDestination && isAtRootOfStack) {
                    ZenithBottomNavigation(
                        currentRoute = navigationState.topLevelRoute,
                        onNavigate = { navigator.navigate(it) }
                    )
                }
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
                                onViewPurchaseOrders = {},
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )
                        }
                        entry<Route.Home.Inventory> {
                            InventoryScreen(
                                onAddProductClick = { navigator.navigate(Route.Home.AddProduct) },
                                businessId = "a6d7b373-52c6-4ae3-84ff-b4bc06d2a46d",
                                onProductClick = {},
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )
                        }
                        entry<Route.Home.Sales> {
                            SalesScreen(
                                businessId = "a6d7b373-52c6-4ae3-84ff-b4bc06d2a46d",
                                onSaleClick = {},
                                onNewSale = {},
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )
                        }
                        entry<Route.Home.Reports> {
                            ReportScreen(
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )
                        }
                        entry<Route.Home.Settings> {
                            SettingsScreen(
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )
                        }
                        entry<Route.Home.Customers> {
                            CustomerListScreen(
                                businessId = "a6d7b373-52c6-4ae3-84ff-b4bc06d2a46d",
                                onCustomerClick = {},
                                onAddCustomer = {},
                                onViewReports = {},
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )
                        }
                        entry<Route.Home.Expenses> {
                            ExpenseListScreen(
                                businessId = "a6d7b373-52c6-4ae3-84ff-b4bc06d2a46d",
                                onAddExpense = {},
                                onExpenseClick = {},
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )
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
}
