package com.techsultan.zenithpro.core.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.techsultan.zenithpro.features.product.presentation.AddProductViewModel
import com.techsultan.zenithpro.features.product.presentation.BarcodeScannerScreen
import com.techsultan.zenithpro.features.product.presentation.InventoryScreen
import com.techsultan.zenithpro.features.production.presentation.ProductionScreen
import com.techsultan.zenithpro.features.sales.presentation.CheckoutScreen
import com.techsultan.zenithpro.features.sales.presentation.CheckoutViewModel
import com.techsultan.zenithpro.features.sales.presentation.SalesScreen
import com.techsultan.zenithpro.features.settings.presentation.BusinessInformationScreen
import com.techsultan.zenithpro.features.settings.presentation.CreateStaffScreen
import com.techsultan.zenithpro.features.settings.presentation.PrinterSettingsScreen
import com.techsultan.zenithpro.features.settings.presentation.SettingsScreen
import com.techsultan.zenithpro.features.settings.presentation.StaffManagementScreen
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

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
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Gesture and Bottom Bar visibility logic
    val isBottomBarDestination = navigationState.topLevelRoute in TOP_LEVEL_DESTINATIONS.keys
    val currentStack = navigationState.backStacks[navigationState.topLevelRoute]
    val isAtRootOfStack = currentStack?.size == 1
    val gesturesEnabled = isBottomBarDestination && isAtRootOfStack

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out") },
            text = { Text("Are you sure you want to log out of your account?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        dataPersistentViewModel.logout()
                    }
                ) {
                    Text("Log out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ZenithNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        onLogout = {
            scope.launch {
                drawerState.close()
                showLogoutDialog = true
            }
        },
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

            val addProductViewModel: AddProductViewModel = koinViewModel()
            val checkoutViewModel: CheckoutViewModel = koinViewModel()

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
                                onLogout = { showLogoutDialog = true },
                                onPrinterSettings = { navigator.navigate(Route.Home.PrinterSettings) },
                                onStaffManagement = { navigator.navigate(Route.Home.StaffManagementScreen) },
                                onBusinessInformation = { navigator.navigate(Route.Home.BusinessInformationScreen) }
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
                            CheckoutScreen(
                                onBack = { navigator.goBack() },
                                onScanBarcode = { navigator.navigate(Route.Home.BarcodeScanner(Route.ScannerCaller.CHECKOUT)) },
                                viewModel = checkoutViewModel,
                                onSaleCompleted = { navigator.goBack() }
                            )
                        }
                        entry<Route.Home.AddProduct> {
                            AddProductScreen(
                                navigateBack = { navigator.goBack() },
                                onScanBarcode = {
                                    navigator.navigate(Route.Home.BarcodeScanner(Route.ScannerCaller.ADD_PRODUCT))
                                },
                                viewModel = addProductViewModel
                            )
                        }
                        entry<Route.Home.BarcodeScanner> { key ->
                            BarcodeScannerScreen(
                                onBarcodeScanned = { barcode ->
                                    when (key.caller) {
                                        Route.ScannerCaller.ADD_PRODUCT -> {
                                            addProductViewModel.onBarcodeScanned(barcode)
                                            navigator.goBack()
                                            true
                                        }
                                        Route.ScannerCaller.CHECKOUT -> {
                                            val found = checkoutViewModel.onBarcodeScanned(barcode)
                                            if (found) {
                                                navigator.goBack()
                                            }
                                            found
                                        }
                                    }
                                },
                                onBack = { navigator.goBack() },
                                onTypeBarcode = { navigator.goBack() }
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
                        entry<Route.Home.BusinessInformationScreen> {
                            BusinessInformationScreen(
                                onBack = { navigator.goBack() }
                            )
                        }
                        entry<Route.Home.StaffManagementScreen> {
                            StaffManagementScreen(
                                onBack = { navigator.goBack() },
                                onAddStaff = { navigator.navigate(Route.Home.CreateStaff)}
                            )
                        }
                        entry<Route.Home.CreateStaff> {
                            CreateStaffScreen(
                                onCreated = { navigator.goBack() },
                                onBack = { navigator.goBack() }
                            )
                        }
                        entry<Route.Home.PrinterSettings> {
                            PrinterSettingsScreen(
                                onBack = { navigator.goBack() }
                            )
                        }
                    }
                )
            )
        }
    }
}
