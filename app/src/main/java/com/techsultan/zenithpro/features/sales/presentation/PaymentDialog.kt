package com.techsultan.zenithpro.features.sales.presentation

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.techsultan.zenithpro.core.components.ZenithButton
import com.techsultan.zenithpro.core.util.Util.formatPrice
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.sales.PaymentMethod
import kotlinx.coroutines.launch

@Composable
fun PaymentDialog(
    totalAmount: String,
    viewModel: CheckoutViewModel,
    onDismiss: () -> Unit,
    onConfirm: (PaymentMethod) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var showSplitDialog by remember { mutableStateOf(false) }
    var showCustomerSelector by remember { mutableStateOf(false) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val splitCashAmount by viewModel.splitCashAmount.collectAsStateWithLifecycle()
    val splitTransferAmount by viewModel.splitTransferAmount.collectAsStateWithLifecycle()

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

                Spacer(modifier = Modifier.height(24.dp))

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
                                // Show customer selector for debt
                                if (state.selectedCustomer == null) {
                                    showCustomerSelector = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Schedule
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Show customer info for debt/split if selected
                if ((selectedMethod == PaymentMethod.DEBT || selectedMethod == PaymentMethod.SPLIT) &&
                    state.selectedCustomer != null) {
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
                } else if ((selectedMethod == PaymentMethod.DEBT || selectedMethod == PaymentMethod.SPLIT) &&
                    state.selectedCustomer == null) {
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
                        Text("Select Customer (Required for ${selectedMethod.name})")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Show split payment summary if selected
                if (selectedMethod == PaymentMethod.SPLIT &&
                    (splitCashAmount > 0 || splitTransferAmount > 0)) {
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
                                Text("₦${splitCashAmount.formatPrice()}", fontWeight = FontWeight.Medium)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Transfer:", style = MaterialTheme.typography.bodySmall)
                                Text("₦${splitTransferAmount.formatPrice()}", fontWeight = FontWeight.Medium)
                            }
                            if (splitCashAmount + splitTransferAmount > 0) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
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

                Spacer(modifier = Modifier.height(24.dp))

                // Confirm Button
                ZenithButton(
                    text = if (selectedMethod == PaymentMethod.SPLIT) "Continue to Split Payment" else "Confirm Payment",
                    onClick = {
                        when (selectedMethod) {
                            PaymentMethod.SPLIT -> {
                                showSplitDialog = true
                            }
                            PaymentMethod.DEBT -> {
                                if (state.selectedCustomer != null) {
                                    onConfirm(selectedMethod)
                                } else {
                                    showCustomerSelector = true
                                }
                            }
                            else -> {
                                onConfirm(selectedMethod)
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
                    onConfirm(PaymentMethod.SPLIT)
                }
            }
        )
    }

    // Customer Selector Dialog
    if (showCustomerSelector) {
        CustomerSelectorDialog(
            viewModel = viewModel,
            onDismiss = { showCustomerSelector = false },
            onCustomerSelected = { customer ->
                viewModel.setCustomer(customer.id, customer)
                showCustomerSelector = false
            }
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
                // Header
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

@Composable
fun CustomerSelectorDialog(
    viewModel: CheckoutViewModel,
    onDismiss: () -> Unit,
    onCustomerSelected: (CustomerEntity) -> Unit
) {
    val searchQuery by viewModel.customerSearchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.customerSearchResults.collectAsStateWithLifecycle()
    val isLoading by remember { mutableStateOf(false) }

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
                Text(
                    text = "Select Customer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onCustomerSearchChanged(it) },
                    placeholder = { Text("Search by name or phone number") },
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

                // Results
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.PersonOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No customers found",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TextButton(
                                            onClick = {
                                                // TODO: Navigate to add customer
                                                onDismiss()
                                            }
                                        ) {
                                            Text("Add new customer")
                                        }
                                    }
                                }
                            }
                        }

                        items(searchResults) { customer ->
                            CustomerSelectionCard(
                                customer = customer,
                                onClick = { onCustomerSelected(customer) }
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
        )
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
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Customer Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.fullName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                customer.phone?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (customer.email != null) {
                    Text(
                        text = customer.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Debt badge if exists
            if (customer.totalDebt > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "₦${customer.totalDebt.formatPrice()}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
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

// Keep your existing PaymentOption composable
@Composable
fun PaymentOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector
) {
    // Your existing PaymentOption implementation
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