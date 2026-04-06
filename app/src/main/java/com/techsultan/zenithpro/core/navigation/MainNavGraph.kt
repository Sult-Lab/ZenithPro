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
import com.techsultan.zenithpro.core.viewmodel.DataPersistentViewModel
import com.techsultan.zenithpro.features.analytics.presentation.ReportScreen
import com.techsultan.zenithpro.features.branch.presentation.BranchScreen
import com.techsultan.zenithpro.features.customer.presentation.AddEditCustomerScreen
import com.techsultan.zenithpro.features.customer.presentation.CustomerDetailScreen
import com.techsultan.zenithpro.features.customer.presentation.CustomerListScreen
import com.techsultan.zenithpro.features.customer.presentation.CustomerReportsScreen
import com.techsultan.zenithpro.features.dashboard.presentation.DashboardScreen
import com.techsultan.zenithpro.features.expenses.presentation.AddEditExpenseScreen
import com.techsultan.zenithpro.features.expenses.presentation.ExpenseListScreen
import com.techsultan.zenithpro.features.material.presentation.MaterialScreen
import com.techsultan.zenithpro.features.product.presentation.AddProductScreen
import com.techsultan.zenithpro.features.product.presentation.InventoryScreen
import com.techsultan.zenithpro.features.production.presentation.ProductionScreen
import com.techsultan.zenithpro.features.sales.presentation.NewSaleScreen
import com.techsultan.zenithpro.features.sales.presentation.SalesScreen
import com.techsultan.zenithpro.features.settings.presentation.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun MainNavGraph(
    dataPersistentViewModel: DataPersistentViewModel
) {
    val navigationState = rememberNavigationState(
        startRoute = Route.Home.Dashboard,
        topLevelDestinations = TOP_LEVEL_DESTINATIONS.keys
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
                navigator.navigate(Route.Customer.CustomerListScreen)
            }
        },
        onExpensesClick = {
            scope.launch {
                drawerState.close()
                navigator.navigate(Route.Expense.ExpenseListScreen)
            }
        },
        onBranchesClick = {
            scope.launch {
                drawerState.close()
                navigator.navigate(Route.Home.Branches)
            }
        },
        onMaterialClick = {
            scope.launch {
                drawerState.close()
                navigator.navigate(Route.Home.Material)
            }
        },
        onProductionClick = {
            scope.launch {
                drawerState.close()
                navigator.navigate(Route.Home.Production)
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
                                onProductClick = {},
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )
                        }
                        entry<Route.Home.Sales> {
                            SalesScreen(
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
                                onBack = {},
                                onAccountSettings = {},
                                onNotifications = {},
                                onAbout = {},
                                onLogout = {}
                            )
                        }

                        entry<Route.Home.Branches> {
                            BranchScreen(
                                onBack = { navigator.goBack() }
                            )
                        }
                        entry<Route.Customer.CustomerListScreen> {
                            CustomerListScreen(
                                onCustomerClick = { navigator.navigate(Route.Customer.CustomerDetailScreen) },
                                onAddCustomer = { navigator.navigate(Route.Customer.AddEditCustomerScreen) },
                                onViewReports = { navigator.navigate(Route.Customer.CustomerReportScreen) },
                                onBack = { navigator.goBack() }
                            )
                        }
                        entry<Route.Expense.ExpenseListScreen> {
                            ExpenseListScreen(
                                onAddExpense = { navigator.navigate(Route.Expense.AddEditExpenseScreen) },
                                onExpenseClick = {},
                                onBack = { navigator.goBack() }
                            )
                        }
                        entry<Route.Expense.AddEditExpenseScreen> {
                            AddEditExpenseScreen(
                                onSaved = {  },
                                onBack = { navigator.goBack() }
                            )
                        }
                        entry<Route.Home.NewSale> {
                            NewSaleScreen(
                                onBack = { navigator.goBack() }
                            )
                        }
                        entry<Route.Home.AddProduct> {
                            AddProductScreen(
                                navigateBack = { navigator.goBack() }
                            )
                        }
                        entry<Route.Home.Production> {
                            ProductionScreen(
                                onBack = { navigator.goBack() }
                            )
                        }
                        entry<Route.Home.Material> {
                            MaterialScreen(
                                onNewProduction = {},
                                onMaterialClick = {},
                                onBack = { navigator.goBack() }
                            )
                        }
                        entry<Route.Customer.AddEditCustomerScreen> {
                            AddEditCustomerScreen(
                                onSaved = { navigator.goBack() },
                                onBack = { navigator.goBack() }
                            )
                        }
                        entry<Route.Customer.CustomerDetailScreen> {
                            CustomerDetailScreen(
                                onEdit = {},
                                onBack = { navigator.goBack() }
                            )
                        }
                        entry<Route.Customer.CustomerReportScreen> {
                            CustomerReportsScreen(
                                onBack = { navigator.goBack() }
                            )
                        }
                    }
                )
            )
        }
    }
}
