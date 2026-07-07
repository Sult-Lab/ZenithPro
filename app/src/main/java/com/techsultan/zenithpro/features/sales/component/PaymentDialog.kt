package com.techsultan.zenithpro.features.sales.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.components.ZenithButton
import com.techsultan.zenithpro.core.util.Util.formatPrice
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.presentation.AddEditCustomerScreen
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.TransferType
import com.techsultan.zenithpro.features.sales.presentation.CheckoutViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDialog(
    totalAmount: String,
    viewModel: CheckoutViewModel,
    onDismiss: () -> Unit,
    onConfirm: (PaymentMethod, TransferType?) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var showSplitDialog by remember { mutableStateOf(false) }
    var showCustomerSelector by remember { mutableStateOf(false) }
    var awaitingSaleId by remember { mutableStateOf<String?>(null) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val splitCashAmount by viewModel.splitCashAmount.collectAsStateWithLifecycle()
    val splitTransferAmount by viewModel.splitTransferAmount.collectAsStateWithLifecycle()

    val needsBranchSelection = state.availableBranches.size > 1 &&
            state.selectedBranchId == null && state.canSelectBranch

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("")
                    Text(
                        text = "Payment Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Total
                Text(
                    text = "Total: $totalAmount",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = "Choose a payment method below",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (state.availableBranches.size > 1) {
                    BranchSelectorRow(
                        selectedBranchName = state.selectedBranchName,
                        isRequired = needsBranchSelection,
                        canSelect = state.canSelectBranch,
                        onSelect = { viewModel.onShowBranchPicker() }
                    )
                    Spacer(Modifier.height(12.dp))
                } else if (state.selectedBranchName != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1976D2).copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                state.selectedBranchName!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1976D2),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1976D2).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    "Auto-selected",
                                    modifier = Modifier.padding(
                                        horizontal = 6.dp, vertical = 2.dp
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1976D2)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaymentOption(
                        title = "Cash",
                        selected = selectedMethod == PaymentMethod.CASH,
                        onClick = {
                            selectedMethod = PaymentMethod.CASH
                            viewModel.setPaymentMethod(PaymentMethod.CASH)
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Money
                    )

                    PaymentOption(
                        title = "Bank Transfer",
                        selected = selectedMethod == PaymentMethod.TRANSFER,
                        onClick = {
                            selectedMethod = PaymentMethod.TRANSFER
                            viewModel.setPaymentMethod(PaymentMethod.TRANSFER)
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AccountBalance
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaymentOption(
                        title = "POS (Card)",
                        selected = selectedMethod == PaymentMethod.POS,
                        onClick = {
                            selectedMethod = PaymentMethod.POS
                            viewModel.setPaymentMethod(PaymentMethod.POS)
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CreditCard
                    )

                    PaymentOption(
                        title = "USSD",
                        selected = selectedMethod == PaymentMethod.USSD,
                        onClick = {
                            selectedMethod = PaymentMethod.USSD
                            viewModel.setPaymentMethod(PaymentMethod.USSD)
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Dialpad
                    )
                }

                // Split Payment Option
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaymentOption(
                        title = "Split",
                        selected = selectedMethod == PaymentMethod.SPLIT,
                        onClick = {
                            selectedMethod = PaymentMethod.SPLIT
                            viewModel.setPaymentMethod(PaymentMethod.SPLIT)
                            showSplitDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.CallSplit
                    )

                    PaymentOption(
                        title = "Debt",
                        selected = selectedMethod == PaymentMethod.DEBT,
                        onClick = {
                            selectedMethod = PaymentMethod.DEBT
                            viewModel.setPaymentMethod(PaymentMethod.DEBT)

                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Schedule
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show customer info
            if (state.selectedCustomer != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Customer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = state.selectedCustomer!!.fullName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (selectedMethod == PaymentMethod.DEBT && state.selectedCustomer!!.totalDebt > 0) {
                                Text(
                                    text = "Existing debt: ₦${state.selectedCustomer!!.totalDebt.formatPrice()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        IconButton(onClick = { showCustomerSelector = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Change",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                TextButton(
                    onClick = { showCustomerSelector = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedMethod == PaymentMethod.DEBT)
                            "Select Customer (Required for Debt)"
                        else "Select Customer (Optional)"
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Show split payment summary if selected
            if (selectedMethod == PaymentMethod.SPLIT &&
                (splitCashAmount > 0 || splitTransferAmount > 0)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Split Payment Breakdown",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cash:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "₦${splitCashAmount.formatPrice()}",
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Transfer:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "₦${splitTransferAmount.formatPrice()}",
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (splitCashAmount + splitTransferAmount > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = DividerDefaults.Thickness,
                                color = DividerDefaults.color
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Paid:", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "₦${(splitCashAmount + splitTransferAmount).formatPrice()}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            state.error?.let { error ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Button
                ZenithButton(
                    text = if (selectedMethod == PaymentMethod.SPLIT) "Continue to Split Payment" else "Confirm Payment",
                    onClick = {
                        when (selectedMethod) {
                            PaymentMethod.TRANSFER -> {
                                if (needsBranchSelection) {
                                    viewModel.onShowBranchPicker()
                                    return@ZenithButton
                                }
                                onConfirm(selectedMethod, null)
                            }
                            PaymentMethod.SPLIT -> {
                                showSplitDialog = true
                            }
                            PaymentMethod.DEBT -> {
                                if (state.selectedCustomer == null){
                                    showCustomerSelector = true
                                    return@ZenithButton
                                }
                                if (needsBranchSelection){
                                    viewModel.onShowBranchPicker()
                                    return@ZenithButton
                                }
                                onConfirm(selectedMethod, null)
                            }
                            else -> {
                                if (needsBranchSelection) {
                                    viewModel.onShowBranchPicker()
                                    return@ZenithButton
                                }
                                onConfirm(selectedMethod, null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Split Payment Dialog
    if (showSplitDialog) {
        SplitPaymentDialog(
            totalAmount = totalAmount,
            viewModel = viewModel,
            onDismiss = { showSplitDialog = false },
            onConfirm = {
                showSplitDialog = false
                if (selectedMethod == PaymentMethod.SPLIT) {
                    onConfirm(PaymentMethod.SPLIT, null)
                }
            }
        )
    }

    if (showCustomerSelector) {
        CustomerSelectorDialog(
            viewModel = viewModel,
            onDismiss = { showCustomerSelector = false },
            onCustomerSelected = { customer ->
                viewModel.setCustomer(customer.id, customer)
                showCustomerSelector = false
            },
        )
    }

    if (state.showBranchPicker) {
        BranchPickerSheet(
            branches   = state.availableBranches,
            selectedId = state.selectedBranchId,
            onSelect   = { branch ->
                viewModel.onBranchSelected(branch)
                // After selecting branch, auto-confirm if method was already chosen
                if (selectedMethod != PaymentMethod.SPLIT &&
                    (selectedMethod != PaymentMethod.DEBT ||
                            state.selectedCustomer != null)) {
                    onConfirm(selectedMethod, null)
                }
            },
            onDismiss  = { viewModel.onDismissBranchPicker() }
        )
    }
}

@Composable
fun SplitPaymentDialog(
    totalAmount: String,
    viewModel: CheckoutViewModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val splitCashAmount by viewModel.splitCashAmount.collectAsStateWithLifecycle()
    val splitTransferAmount by viewModel.splitTransferAmount.collectAsStateWithLifecycle()
    val totalAmountLong = totalAmount.replace("₦", "").replace(",", "").toLongOrNull() ?: 0L

    var cashInput by remember { mutableStateOf(if (splitCashAmount > 0) splitCashAmount.toString() else "") }
    var transferInput by remember { mutableStateOf(if (splitTransferAmount > 0) splitTransferAmount.toString() else "") }

    val cashAmount = cashInput.toLongOrNull() ?: 0L
    val transferAmount = transferInput.toLongOrNull() ?: 0L
    val totalPaid = cashAmount + transferAmount
    val remaining = maxOf(0L, totalAmountLong - totalPaid)
    val change = maxOf(0L, totalPaid - totalAmountLong)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Split Payment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Box(modifier = Modifier.size(40.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Total
                Text(
                    text = "Total: $totalAmount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Cash Input
                OutlinedTextField(
                    value = cashInput,
                    onValueChange = {
                        cashInput = it.filter { char -> char.isDigit() }
                        val cash = cashInput.toLongOrNull() ?: 0L
                        viewModel.setSplitAmounts(cash, transferAmount)
                    },
                    label = { Text("Cash Amount") },
                    prefix = { Text("₦") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Money,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Transfer Input
                OutlinedTextField(
                    value = transferInput,
                    onValueChange = {
                        transferInput = it.filter { char -> char.isDigit() }
                        val transfer = transferInput.toLongOrNull() ?: 0L
                        viewModel.setSplitAmounts(cashAmount, transfer)
                    },
                    label = { Text("Transfer Amount") },
                    prefix = { Text("₦") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Summary Surface
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (remaining > 0)
                        MaterialTheme.colorScheme.errorContainer
                    else if (change > 0)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Paid:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "₦${totalPaid.formatPrice()}",
                                fontWeight = FontWeight.Bold,
                                color = if (totalPaid > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (remaining > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Remaining:", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "₦${remaining.formatPrice()}",
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        if (change > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Change:", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "₦${change.formatPrice()}",
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Confirm Button
                ZenithButton(
                    text = "Confirm Split Payment",
                    onClick = {
                        if (totalPaid >= totalAmountLong) {
                            onConfirm()
                        }
                    },
                    enabled = totalPaid >= totalAmountLong,
                    modifier = Modifier.fillMaxWidth()
                )

                if (totalPaid < totalAmountLong) {
                    Text(
                        text = "Please enter total amount that covers or exceeds the total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSelectorDialog(
    viewModel: CheckoutViewModel,
    onDismiss: () -> Unit,
    onCustomerSelected: (CustomerEntity) -> Unit,
) {
    var showAddCustomerSheet by remember { mutableStateOf(false) }
    val searchQuery by viewModel.customerSearchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.customerSearchResults.collectAsStateWithLifecycle()
    val newlyCreatedCustomer by viewModel.newlyCreatedCustomer.collectAsStateWithLifecycle()

    // Auto-select and close when a new customer is created
    LaunchedEffect(newlyCreatedCustomer) {
        newlyCreatedCustomer?.let { customer ->
            onCustomerSelected(customer)
            viewModel.clearNewlyCreatedCustomer()
            showAddCustomerSheet = false
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Customer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onCustomerSearchChanged(it) },
                    placeholder = {
                        Text(
                            text = "Search by name or phone number",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                                  },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onCustomerSearchChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.onCustomerSearchChanged("") // Clear search
                        showAddCustomerSheet = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add New Customer",
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Results Label
                if (searchQuery.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Search Results (${searchResults.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchResults.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.onCustomerSearchChanged("") }
                            ) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Results
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (searchQuery.isBlank()) {
                        item {
                            EmptyCustomerSearchState()
                        }
                    } else if (searchResults.isEmpty()) {
                        item {
                           CustomerNotFoundState(
                               searchQuery = searchQuery,
                               onAddCustomer = { showAddCustomerSheet = true }
                           )
                        }
                    } else {
                        items(searchResults, key = { it.id }) { customer ->
                            CustomerSelectionCard(
                                customer = customer,
                                onClick = {
                                    onCustomerSelected(customer)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }

    if (showAddCustomerSheet) {

        ModalBottomSheet(
            onDismissRequest = { showAddCustomerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {

            AddCustomerBottomSheetContent(
                prefilledPhone = if (searchQuery.all { it.isDigit() }) {
                    searchQuery
                } else {
                    ""
                },
                onSaved = {
                    showAddCustomerSheet = false
                }
            )
        }
    }
}

@Composable
fun CustomerSelectionCard(
    customer: CustomerEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = customer.fullName.take(2).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp
                )
            }


            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.fullName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                if (customer.phone != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (customer.email != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = customer.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (customer.totalDebt > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Debt",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "₦${customer.totalDebt.formatPrice()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${customer.visitCount} visits",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PaymentOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector
) {

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BranchSelectorRow(
    selectedBranchName: String?,
    isRequired: Boolean,
    canSelect: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canSelect, onClick = onSelect),
        shape  = RoundedCornerShape(10.dp),
        color  = when {
            selectedBranchName != null -> Color(0xFF1976D2).copy(alpha = 0.08f)
            isRequired  -> MaterialTheme.colorScheme.errorContainer
                .copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            width = 1.dp,
            color = when {
                selectedBranchName != null -> Color(0xFF1976D2).copy(alpha = 0.3f)
                isRequired -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                tint = when {
                    selectedBranchName != null -> Color(0xFF1976D2)
                    isRequired -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
            )
            Text(
                selectedBranchName ?: if (isRequired) "Select branch (required)" else "Select branch",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selectedBranchName != null) FontWeight.SemiBold
                else FontWeight.Normal,
                color = when {
                    selectedBranchName != null -> Color(0xFF1976D2)
                    isRequired -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f)
            )
            if (canSelect) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

