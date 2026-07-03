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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.techsultan.zenithpro.core.components.ZenithBottomNavigation
import com.techsultan.zenithpro.core.components.ZenithNavigationDrawer
import com.techsultan.zenithpro.core.viewmodel.DataPersistentViewModel
import com.techsultan.zenithpro.features.analytics.presentation.ReportsScreen
import com.techsultan.zenithpro.features.branch.presentation.BranchScreen
import com.techsultan.zenithpro.features.customer.presentation.AddEditCustomerScreen
import com.techsultan.zenithpro.features.customer.presentation.CustomerDetailScreen
import com.techsultan.zenithpro.features.customer.presentation.CustomerListScreen
import com.techsultan.zenithpro.features.customer.presentation.CustomerReportsScreen
import com.techsultan.zenithpro.features.dashboard.presentation.DashboardScreen
import com.techsultan.zenithpro.features.expenses.presentation.AddEditExpenseScreen
import com.techsultan.zenithpro.features.expenses.presentation.ExpenseListScreen
import com.techsultan.zenithpro.features.material.presentation.MaterialScreen
import com.techsultan.zenithpro.features.inventory.presentation.AddProductScreen
import com.techsultan.zenithpro.features.inventory.presentation.AddProductViewModel
import com.techsultan.zenithpro.features.inventory.presentation.BarcodeScannerScreen
import com.techsultan.zenithpro.features.inventory.presentation.InventoryScreen
import com.techsultan.zenithpro.features.inventory.presentation.PrintBarcodeScreen
import com.techsultan.zenithpro.features.inventory.presentation.ProductDetailScreen
import com.techsultan.zenithpro.features.inventory.presentation.ProductDetailViewModel
import com.techsultan.zenithpro.features.production.presentation.ProductionScreen
import com.techsultan.zenithpro.features.sales.presentation.AwaitingTransferScreen
import com.techsultan.zenithpro.features.sales.presentation.CheckoutScreen
import com.techsultan.zenithpro.features.sales.presentation.CheckoutViewModel
import com.techsultan.zenithpro.features.sales.presentation.ReceiptPreviewScreen
import com.techsultan.zenithpro.features.sales.presentation.ReceiptViewModel
import com.techsultan.zenithpro.features.sales.presentation.SaleDetailScreen
import com.techsultan.zenithpro.features.sales.presentation.SalesScreen
import com.techsultan.zenithpro.features.settings.presentation.BusinessInformationScreen
import com.techsultan.zenithpro.features.settings.presentation.BusinessProfileScreen
import com.techsultan.zenithpro.features.settings.presentation.CategoryManagementScreen
import com.techsultan.zenithpro.features.settings.presentation.CreateStaffScreen
import com.techsultan.zenithpro.features.settings.presentation.PaymentSettingsScreen
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

    val session by dataPersistentViewModel.session.collectAsStateWithLifecycle()

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
        session = session,
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
            val productDetailViewModel: ProductDetailViewModel = koinViewModel()
            val checkoutViewModel: CheckoutViewModel = koinViewModel()
            val receiptViewModel: ReceiptViewModel = koinViewModel()

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
                                onViewReports = { navigator.navigate(Route.Home.Reports) },
                                onViewSales = { navigator.navigate(Route.Home.Sales) },
                                onViewDebts = {},
                                onViewLowStock = {},
                                onViewPurchaseOrders = {},
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )
                        }
                        entry<Route.Home.Inventory> {
                            InventoryScreen(
                                onAddProductClick = { navigator.navigate(Route.Home.AddProduct) },
                                onProductClick = { navigator.navigate(Route.Home.ProductDetail(it)) },
                                onMenuClick = { scope.launch { drawerState.open() } },
                                onPrintBarcodeClick = { navigator.navigate(Route.Home.PrintBarcode) }
                            )
                        }
                        entry<Route.Home.Sales> {
                            SalesScreen(
                                onSaleClick = { navigator.navigate(Route.Home.SaleDetail(it)) },
                                onNewSale = { navigator.navigate(Route.Home.NewSale) },
                                onMenuClick = { scope.launch { drawerState.open() } },
                                onReceiptPreview = { receipt ->
                                    receiptViewModel.setReceipt(receipt)
                                    navigator.navigate(Route.Home.ReceiptPreview)
                                }
                            )
                        }
                        entry<Route.Home.Reports> {
                            ReportsScreen(
                                onMenuClick = { scope.launch { drawerState.open() } },
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
                                onBusinessInformation = { navigator.navigate(Route.Home.BusinessInformationScreen) },
                                onBusinessProfile = { navigator.navigate(Route.Home.BusinessProfileScreen) },
                                onCategoryManagement = { navigator.navigate(Route.Home.CategoryManagementScreen) },
                                onPaymentSettings = { navigator.navigate(Route.Home.PaymentSettings) }
                            )
                        }

                        entry<Route.Home.Branches> {
                            BranchScreen(
                                onBack = { navigator.goBack() }
                            )
                        }
                        entry<Route.Customer.CustomerListScreen> {
                            CustomerListScreen(
                                onCustomerClick = { navigator.navigate(Route.Customer.CustomerDetailScreen(it)) },
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
                                onSaleCompleted = { navigator.goBack() },
                                onReceiptPreview = { receipt ->
                                    receiptViewModel.setReceipt(receipt)
                                    navigator.navigate(Route.Home.ReceiptPreview)
                                },
                                onAwaitingTransfer = { event ->
                                    navigator.navigate(
                                        Route.Home.AwaitingTransfer(
                                            saleId = event.saleId,
                                            totalAmount = event.totalAmount,
                                            paymentReference = event.paymentReference,
                                            virtualAccountNumber = event.virtualAccountNumber,
                                            virtualAccountBank = event.virtualAccountBank,
                                            virtualAccountName = event.virtualAccountName
                                        )
                                    )
                                }
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
                        entry<Route.Home.ProductDetail> { key ->
                            ProductDetailScreen(
                                productId = key.productId,
                                onBack = { navigator.goBack() },
                                onScanBarcode = {
                                    navigator.navigate(Route.Home.BarcodeScanner(Route.ScannerCaller.PRODUCT_DETAIL))
                                },
                                viewModel = productDetailViewModel
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
                                        Route.ScannerCaller.PRODUCT_DETAIL -> {
                                            productDetailViewModel.onBarcodeScanned(barcode)
                                            navigator.goBack()
                                            true
                                        }
                                    }
                                },
                                onBack = { navigator.goBack() },
                                onTypeBarcode = { navigator.goBack() }
                            )
                        }
                        entry<Route.Home.PrintBarcode> {
                            PrintBarcodeScreen(
                                onBackClick = { navigator.goBack() }
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
                        entry<Route.Customer.CustomerDetailScreen> { key ->
                            CustomerDetailScreen(
                                onEdit = {},
                                onBack = { navigator.goBack() },
                                customerId = key.customerId
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
                        entry<Route.Home.BusinessProfileScreen> {
                            BusinessProfileScreen(
                                onBack = { navigator.goBack() },
                                onSaveChanges = { navigator.goBack() }
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
                        entry<Route.Home.CategoryManagementScreen> {
                            CategoryManagementScreen(
                                onBack = { navigator.goBack() }
                            )
                        }
                        entry<Route.Home.PaymentSettings> {
                            PaymentSettingsScreen(
                                onBack = { navigator.goBack() }
                            )
                        }
                        entry<Route.Home.ReceiptPreview> {
                            ReceiptPreviewScreen(
                                onBack = { navigator.goBack() },
                                viewModel = receiptViewModel
                            )
                        }
                        entry<Route.Home.SaleDetail> { key ->
                            SaleDetailScreen(
                                saleId = key.saleId,
                                businessId = session?.businessId ?: "",
                                onBack = { navigator.goBack() }
                            )
                        }
                        entry<Route.Home.AwaitingTransfer> { key ->
                            AwaitingTransferScreen(
                                saleId = key.saleId,
                                totalAmount = key.totalAmount,
                                paymentReference = key.paymentReference,
                                virtualAccountNumber = key.virtualAccountNumber,
                                virtualAccountBank = key.virtualAccountBank,
                                virtualAccountName = key.virtualAccountName,
                                businessId = session?.businessId ?: "",
                                onConfirmed = {
                                    navigator.navigate(Route.Home.SaleDetail(key.saleId))
                                },
                                onCancel = { navigator.goBack() }
                            )
                        }
                    }
                )
            )
        }
    }
}
