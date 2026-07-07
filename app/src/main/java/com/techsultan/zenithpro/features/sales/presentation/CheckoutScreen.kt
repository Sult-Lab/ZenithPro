package com.techsultan.zenithpro.features.sales.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.core.data.UserSession
import com.techsultan.zenithpro.core.data.local.ReceiptData
import com.techsultan.zenithpro.core.util.Util.formatPrice
import com.techsultan.zenithpro.features.inventory.data.local.ProductWithVariants
import com.techsultan.zenithpro.features.payment.presentation.TransferPaymentViewModel
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.TransferType
import com.techsultan.zenithpro.features.sales.component.BranchPickerSheet
import com.techsultan.zenithpro.features.sales.component.BranchSelectorChip
import com.techsultan.zenithpro.features.sales.component.ManualTransferConfirmScreen
import com.techsultan.zenithpro.features.sales.component.PaymentDialog
import com.techsultan.zenithpro.features.sales.component.TransferTypePickerSheet
import com.techsultan.zenithpro.features.sales.data.remote.CartItem
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit = {},
    onScanBarcode: () -> Unit = {},
    viewModel: CheckoutViewModel,
    session: UserSession? = null,
    transferPaymentViewModel: TransferPaymentViewModel = koinViewModel(),
    onSaleCompleted: (saleId: String) -> Unit,
    onReceiptPreview: (ReceiptData) -> Unit = {},
    onAwaitingTransferConfirmed: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val cartTotal by viewModel.cartTotal.collectAsStateWithLifecycle()
    val cartItemCount by viewModel.cartItemCount.collectAsStateWithLifecycle()
    val currentTerminal by viewModel.currentTerminal.collectAsStateWithLifecycle()
    var showTransferPicker by remember { mutableStateOf(false) }
    val transferState by transferPaymentViewModel.uiState.collectAsStateWithLifecycle()
    var selectedTransferType by remember { mutableStateOf<TransferType?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    var showManualTransferScreen by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var awaitingTransferEvent by remember { mutableStateOf<CheckoutViewModel.CheckoutEvent.AwaitingTransfer?>(null) }
    var pendingSaleId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val vatAmount = (cartTotal * (state.taxRate / 100)).toLong()
    val grandTotal = cartTotal + vatAmount

    LaunchedEffect(Unit) {
        viewModel.loadTerminalForCurrentBranch()
    }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CheckoutViewModel.CheckoutEvent.SaleCompleted -> {

                }
                is CheckoutViewModel.CheckoutEvent.ReceiptReady-> {
                    onReceiptPreview(event.receipt)
                    snackbarHost.showSnackbar("Payment successful")
                }
                is CheckoutViewModel.CheckoutEvent.ShowError -> {
                    snackbarHost.showSnackbar(event.message)
                }
                is CheckoutViewModel.CheckoutEvent.ProductAddedByBarcode -> {
                    snackbarHost.showSnackbar("Added ${event.productName} to cart")
                }

                is CheckoutViewModel.CheckoutEvent.AwaitingTransfer -> {
                    awaitingTransferEvent = event
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            ZenithTopAppBar(
                title = "New Sale",
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (cartItemCount > 0) {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { showBottomSheet = true }
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier) {
                        if (state.availableBranches.isNotEmpty() &&
                            state.selectedBranchId == null){
                            Surface(
                                shape  = RoundedCornerShape(8.dp),
                                color  = Color(0xFFD32F2F).copy(alpha = 0.08f),
                                border = BorderStroke(
                                    1.dp, Color(0xFFD32F2F).copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, null,
                                        tint     = Color(0xFFD32F2F),
                                        modifier = Modifier.size(16.dp))
                                    Text("Select a branch to continue",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD32F2F),
                                        modifier = Modifier.weight(1f))
                                    TextButton(
                                        onClick = { viewModel.onShowBranchPicker() },
                                        colors  = ButtonDefaults.textButtonColors(
                                            contentColor = Color(0xFFD32F2F)
                                        )
                                    ) { Text("Select") }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Cart",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$cartItemCount Items",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Total: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "₦${grandTotal.formatPrice()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.ExpandLess,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Search for a product...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isEmpty()) {
                        IconButton(onClick = onScanBarcode) {
                            Icon(
                                imageVector = Icons.Outlined.QrCodeScanner,
                                contentDescription = "Scan",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )

            // Category Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories
                    .filter { it.isNotBlank() }
                    .forEach { category ->
                        CategoryChip(
                            text = category,
                            selected = state.selectedCategory == category,
                            onClick = { viewModel.onCategoryFilterChanged(category) }
                        )
                    }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading && filteredProducts.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.availableBranches.isNotEmpty()) {
                            item {
                                BranchSelectorChip(
                                    selectedBranchName = state.selectedBranchName,
                                    canSelect          = state.canSelectBranch,
                                    onClick            = { viewModel.onShowBranchPicker() }
                                )
                            }
                        }
                        items(
                            items = filteredProducts,
                            key = { it.product.id }
                        ) { productWithVariants ->
                            val cartItem = cart[productWithVariants.product.id]
                            SaleProductItemCard(
                                productWithVariants = productWithVariants,
                                quantityInCart = cartItem?.quantity ?: 0,
                                onAdd = { viewModel.addToCart(productWithVariants) },
                                onRemove = { viewModel.removeFromCart(productWithVariants.product.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showBranchPicker) {
        BranchPickerSheet(
            branches   = state.availableBranches,
            selectedId = state.selectedBranchId,
            onSelect   = { viewModel.onBranchSelected(it) },
            onDismiss  = { viewModel.onDismissBranchPicker() }
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            CartBottomSheetContent(
                cartItems = cart.values.toList(),
                subTotal = cartTotal,
                taxRate = state.taxRate,
                onCheckout = { 
                    showBottomSheet = false
                    showPaymentDialog = true 
                }
            )
        }
    }

    if (showPaymentDialog) {
        PaymentDialog(
            totalAmount = "₦${grandTotal.formatPrice()}",
            viewModel = viewModel,
            onDismiss = {
                showPaymentDialog = false
                viewModel.clearCustomer()
            },
            onConfirm = { selectedMethod, transferType ->
                when {
                    selectedMethod == PaymentMethod.TRANSFER -> {
                        showPaymentDialog = false
                        showTransferPicker = true
                    }
                    else -> {
                        viewModel.checkout()
                        showPaymentDialog = false
                    }
                }
            }
        )
    }

    if (showTransferPicker) {
        TransferTypePickerSheet(
            hasNombaAccount = currentTerminal?.nombaVirtualAccountNumber != null,
            onSelectManual = {
                selectedTransferType = TransferType.MANUAL
                showTransferPicker = false
                viewModel.checkout(transferType = TransferType.MANUAL)
            },
            onSelectNomba = {
                selectedTransferType = TransferType.NOMBA
                showTransferPicker = false
                viewModel.checkout(transferType = TransferType.NOMBA)
            },
            onDismiss = {
                showTransferPicker = false
                showPaymentDialog = true
            }
        )
    }

    if (showManualTransferScreen) {
        ManualTransferConfirmScreen(
            totalAmount = cartTotal,
            businessBankName = "",
            businessAccountNumber = "",
            businessAccountName = "",
            onConfirmReceived = {
                // Sale already processed — just mark payment as received
                showManualTransferScreen = false

            },
            onCancel = {
                showManualTransferScreen = false
                // Sale is already saved as AWAITING_PAYMENT — it stays that way
                // Cashier can reconcile later

            }
        )
    }

    awaitingTransferEvent?.let { event ->
        AwaitingTransferSheet(
            saleId = event.saleId,
            totalAmount = event.totalAmount,
            paymentReference = event.paymentReference,
            virtualAccountNumber = event.virtualAccountNumber,
            virtualAccountBank = event.virtualAccountBank,
            virtualAccountName = event.virtualAccountName,
            businessId = session?.businessId ?: "",
            onConfirmed = {
                awaitingTransferEvent = null
                onAwaitingTransferConfirmed(event.saleId)
            },
            onDismiss = { awaitingTransferEvent = null }
        )
    }
}

@Composable
fun CategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SaleProductItemCard(
    productWithVariants: ProductWithVariants,
    quantityInCart: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val product = productWithVariants.product
    val totalStock = productWithVariants.variants.sumOf { v -> v.stock.sumOf { it.quantity } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.imageUrls.firstOrNull(),
                contentDescription = product.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₦${product.baseSalesPrice.formatPrice()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Stock: $totalStock",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (totalStock > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            }

            if (quantityInCart > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = quantityInCart.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    IconButton(
                        onClick = onAdd,
                        enabled = totalStock > quantityInCart,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Button(
                    onClick = onAdd,
                    enabled = totalStock > 0,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Add")
                }
            }
        }
    }
}

@Composable
fun CartBottomSheetContent(
    cartItems: List<CartItem>,
    subTotal: Long,
    taxRate: Double,
    onCheckout: () -> Unit
) {
    val vatAmount = (subTotal * (taxRate / 100)).toLong()
    val totalAmount = subTotal + vatAmount

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Cart Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cartItems) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = item.productName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "${item.quantity} x ₦${item.unitPrice.formatPrice()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "₦${(item.unitPrice * item.quantity).formatPrice()}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Sub Total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sub Total",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "₦${subTotal.formatPrice()}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        if (taxRate > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VAT (${taxRate}%)",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "₦${vatAmount.formatPrice()}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total Amount",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "₦${totalAmount.formatPrice()}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCheckout,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Proceed to Checkout", modifier = Modifier.padding(vertical = 8.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
