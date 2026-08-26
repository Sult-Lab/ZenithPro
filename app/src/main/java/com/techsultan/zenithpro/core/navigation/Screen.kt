package com.techsultan.zenithpro.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route: NavKey {

    @Serializable
    data object Auth : Route, NavKey {
        @Serializable
        data object SignIn : Route, NavKey
        @Serializable
        data object SignUp : Route, NavKey
        @Serializable
        data class ChangePassword(
            val mode: PasswordChangeMode = PasswordChangeMode.CHANGE,
            val email: String? = null
        ) : Route, NavKey
    }

    @Serializable
    enum class PasswordChangeMode {
        CHANGE, FORCED, FORGOT
    }

    @Serializable
    data object Home : Route, NavKey {
        @Serializable
        data object Dashboard : Route, NavKey
        @Serializable
        data object Inventory : Route, NavKey
        @Serializable
        data object Sales : Route, NavKey
        @Serializable
        data object Reports : Route, NavKey
        @Serializable
        data object Settings : Route, NavKey
        @Serializable
        data object AddProduct : Route, NavKey
        @Serializable
        data class ProductDetail(val productId: String) : Route, NavKey
        @Serializable
        data object NewSale : Route, NavKey
        @Serializable
        data object Branches : Route, NavKey
        @Serializable
        data object Production : Route, NavKey
        @Serializable
        data object Material : Route, NavKey

        @Serializable
        data object CreateStaff : Route, NavKey

        @Serializable
        data object BusinessInformationScreen : Route, NavKey
        @Serializable
        data object BusinessProfileScreen : Route, NavKey
        @Serializable
        data object StaffManagementScreen : Route, NavKey

        @Serializable
        data object PrinterSettings : Route, NavKey

        @Serializable
        data class BarcodeScanner(val caller: ScannerCaller) : Route, NavKey

        @Serializable
        data object PrintBarcode : Route, NavKey

        @Serializable
        data object CategoryManagementScreen : Route, NavKey

        @Serializable
        data object ReceiptPreview : Route, NavKey

        @Serializable
        data object PaymentSettings : Route, NavKey

        @Serializable
        data class SaleDetail(val saleId: String) : Route, NavKey

        @Serializable
        data object NombaTransferScreen: Route, NavKey
    }

    @Serializable
    data object Expense : Route, NavKey {
        @Serializable
        data object ExpenseListScreen : Route, NavKey
        @Serializable
        data object AddEditExpenseScreen : Route, NavKey
    }

    @Serializable
    data object Customer : Route, NavKey {
        @Serializable
        data object CustomerListScreen : Route, NavKey
        @Serializable
        data object CustomerReportScreen : Route, NavKey
        @Serializable
        data class CustomerDetailScreen(val customerId: String) : Route, NavKey
        @Serializable
        data object AddEditCustomerScreen : Route, NavKey
    }

    @Serializable
    enum class ScannerCaller {
        ADD_PRODUCT, CHECKOUT, PRODUCT_DETAIL
    }

}