package com.techsultan.zenithpro.features.customer.presentation

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.core.util.Util.trimOrNull
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.data.local.DebtPaymentEntity
import com.techsultan.zenithpro.features.customer.presentation.viewmodel.CustomerDetailViewModel
import com.techsultan.zenithpro.features.customer.util.CustomerTransaction
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.SaleStatus
import com.techsultan.zenithpro.features.sales.data.local.SaleEntity
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import com.techsultan.zenithpro.features.sales.formatAmount
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    viewModel: CustomerDetailViewModel = koinViewModel(),
    onEdit: (CustomerEntity) -> Unit,
    onBack: () -> Unit,
    customerId: String
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val session = viewModel.session
    val snackbarHost = remember { SnackbarHostState() }
    var showPaymentSheet by remember { mutableStateOf<SaleWithItems?>(null) }

    LaunchedEffect(customerId) {
        viewModel.init(customerId, session?.businessId ?: "")
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CustomerDetailViewModel.CustomerDetailEvent.PaymentRecorded ->
                    snackbarHost.showSnackbar("Payment recorded")
                is CustomerDetailViewModel.CustomerDetailEvent.ShowError ->
                    snackbarHost.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            ZenithTopAppBar(
                title = state.customer?.fullName ?: "Customer",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Menu")
                    }
                },
                actions = {
                    state.customer?.let { customer ->
                        IconButton(onClick = { onEdit(customer) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            state.customer?.let { customer ->
                item {
                    CustomerStatsRow(customer = customer)
                }

                if (customer.totalDebt > 0) {
                    item {
                        DebtAlertCard(
                            customer  = customer,
                            debtSales = state.transactions
                                .filterIsInstance<CustomerTransaction.Sale>()
                                .map { it.saleWithItems }
                                .filter {
                                    it.sale.debtAmount > 0 &&
                                            it.sale.status == SaleStatus.PARTIAL
                                },
                            onRecordPayment = { sale -> showPaymentSheet = sale }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Transaction history",
                    style  = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (state.transactions.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = "No transactions yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = state.transactions,
                    key = { tx ->
                        when (tx) {
                            is CustomerTransaction.Sale -> "sale_${tx.saleWithItems.sale.id}"
                            is CustomerTransaction.DebtPayment -> "payment_${tx.payment.id}"
                        }
                    }
                ) { transaction ->
                    when(transaction){
                        is CustomerTransaction.Sale -> CustomerTransactionCard(
                            saleWithItems = transaction.saleWithItems,
                            onRecordPayment = if (transaction.saleWithItems.sale.debtAmount > 0){
                                { showPaymentSheet = transaction.saleWithItems }
                            } else {
                                null
                            }
                        )
                        is CustomerTransaction.DebtPayment -> DebtPaymentCard(
                            payment = transaction.payment,
                            originalSale = transaction.originalSale
                        )
                    }
                }
            }
        }
    }

    showPaymentSheet?.let { sale ->
        DebtPaymentSheet(
            sale      = sale,
            onConfirm = { amount, method, notes ->
                viewModel.recordPayment(sale.sale.id, amount, method, notes)
                showPaymentSheet = null
            },
            onDismiss = { showPaymentSheet = null }
        )
    }
}

@Composable
private fun CustomerStatsRow(customer: CustomerEntity) {
    Row(
        modifier  = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(
            Triple("Total spent",  "₦${customer.totalSpent.formatAmount()}", Color(0xFF1976D2)),
            Triple("Visits",       customer.visitCount.toString(),           Color(0xFF7B1FA2)),
            Triple("Debt",         "₦${customer.totalDebt.formatAmount()}",
                if (customer.totalDebt > 0) Color(0xFFD32F2F) else Color(0xFF388E3C))
        ).forEach { (label, value, color) ->
            Card(
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier            = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text       = value,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = color
                    )
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtAlertCard(
    customer: CustomerEntity,
    debtSales: List<SaleWithItems>,
    onRecordPayment: (SaleWithItems) -> Unit,
) {
    if (debtSales.isEmpty()) {
        Surface(
            shape  = RoundedCornerShape(12.dp),
            color  = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color(0xFFD32F2F),
                    strokeWidth = 2.dp
                )
                Text(
                    "Outstanding debt: ₦${customer.totalDebt.formatAmount()}",
                    style  = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color  = MaterialTheme.colorScheme.errorContainer
                )
            }
        }
        return
    }

    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Outstanding debt: ₦${customer.totalDebt.formatAmount()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            debtSales.forEach { sale ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = "₦${sale.sale.debtAmount.formatAmount()} — ${
                            Instant.parse(sale.sale.soldAt)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(
                        onClick = { onRecordPayment(sale) },
                        colors  = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Text(
                            text = "Pay", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerTransactionCard(
    saleWithItems: SaleWithItems,
    onRecordPayment: (() -> Unit)?,
) {
    val sale = saleWithItems.sale
    Log.d("Customer Detail Screen", "CustomerTransactionCard: $saleWithItems")
    Log.d("Customer Detail Screen", "CustomerTransactionCard: ${saleWithItems.items.joinToString(", ")}")
    Card(
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text  = Instant.parse(sale.soldAt)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₦${sale.totalAmount.formatAmount()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text  = saleWithItems.items.joinToString(", ") {
                    "${it.productName} ×${it.quantity}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (sale.debtAmount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = "₦${sale.debtAmount.formatAmount()} unpaid",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    onRecordPayment?.let {
                        TextButton(
                            onClick = it,
                            colors  = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = "Record payment",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DebtPaymentCard(
    payment: DebtPaymentEntity,
    originalSale: SaleEntity?,
) {
    Card(
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(
            containerColor = Color(0xFF388E3C).copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, Color(0xFF388E3C).copy(alpha = 0.2f))
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = Color(0xFF388E3C),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Debt payment",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF388E3C)
                )
                originalSale?.let { sale ->
                    Text(
                        "For sale of ₦${sale.totalAmount.formatAmount()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        Instant.parse(payment.paidAt)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            payment.paymentMethod.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(
                                horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                payment.notes?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            Text(
                "+₦${payment.amount.formatAmount()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtPaymentSheet(
    sale: SaleWithItems,
    onConfirm: (Long, PaymentMethod, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText    by remember { mutableStateOf("") }
    var method        by remember { mutableStateOf(PaymentMethod.CASH) }
    var notes         by remember { mutableStateOf("") }
    val outstanding   = sale.sale.debtAmount

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text       = "Record debt payment",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  = "Outstanding: ₦${outstanding.formatAmount()}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD32F2F)
            )
            OutlinedTextField(
                value         = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                label         = { Text("Amount paid") },
                prefix        = { Text("₦") },
                modifier      = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape         = RoundedCornerShape(10.dp)
            )
            // Payment method chips
            Text("Payment method", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(PaymentMethod.CASH, PaymentMethod.POS, PaymentMethod.TRANSFER)) { m ->
                    FilterChip(
                        selected = method == m,
                        onClick  = { method = m },
                        label    = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            OutlinedTextField(
                value         = notes,
                onValueChange = { notes = it },
                label         = { Text("Notes (optional)") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(10.dp)
            )
            Button(
                onClick  = {
                    val amount = amountText.toLongOrNull() ?: return@Button
                    if (amount > 0 && amount <= outstanding) {
                        onConfirm(amount, method, notes.trimOrNull())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                enabled  = (amountText.toLongOrNull() ?: 0L) in 1..outstanding
            ) {
                Text("Confirm payment", fontWeight = FontWeight.Bold)
            }
        }
    }
}