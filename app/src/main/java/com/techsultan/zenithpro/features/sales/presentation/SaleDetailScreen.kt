package com.techsultan.zenithpro.features.sales.presentation

import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.components.ZenithButton
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.core.domain.domain.UnitType
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.features.sales.FulfillmentStatus
import com.techsultan.zenithpro.features.sales.PaymentMethod
import com.techsultan.zenithpro.features.sales.data.local.SaleEntity
import com.techsultan.zenithpro.features.sales.data.local.SaleItemEntity
import com.techsultan.zenithpro.features.sales.data.local.SaleWithItems
import com.techsultan.zenithpro.features.sales.formatAmount
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SaleDetailScreen(
    saleId: String,
    businessId: String,
    viewModel: SaleDetailViewModel = koinViewModel(),
    onBack: () -> Unit,
) {
    LaunchedEffect(saleId) { viewModel.load(saleId, businessId) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SaleDetailViewModel.SaleDetailEvent.PaymentConfirmed ->
                    snackbar.showSnackbar("Payment confirmed!")
                is SaleDetailViewModel.SaleDetailEvent.StatusUpdated ->
                    snackbar.showSnackbar("Fulfillment status updated!")
                is SaleDetailViewModel.SaleDetailEvent.ShowError ->
                    snackbar.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ZenithTopAppBar(
                title = "Sale details",
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator()
                }

                state.error != null -> {
                    Text(
                        text = state.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                state.saleWithItems != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ── Sale summary ──────────────────────────────────────────
                        item {
                            SaleSummaryCard(
                                saleWithItems = state.saleWithItems!!,
                                cashierName = state.cashierName
                            )
                        }

                        // ── Fulfillment status card ────────────────────────────────
                        item {
                            FulfillmentStatusCard(
                                fulfillmentStatus = state.saleWithItems!!.sale.fulfillmentStatus,
                                canUpdate = state.canUpdateFulfillment,
                                isUpdating = state.isUpdating,
                                onUpdateStatus = { newStatus ->
                                    viewModel.updateFulfillmentStatus(newStatus)
                                }
                            )
                        }

                        // ── Items ─────────────────────────────────────────────────
                        item {
                            Text(
                                text = "Items",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        items(
                            items = state.saleWithItems!!.items
                        ) { item ->
                            SaleItemRow(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FulfillmentStatusCard(
    fulfillmentStatus: FulfillmentStatus,
    canUpdate: Boolean,
    isUpdating: Boolean,
    onUpdateStatus: (FulfillmentStatus) -> Unit
) {
    val isTerminal = fulfillmentStatus in listOf(
        FulfillmentStatus.DELIVERED,
        FulfillmentStatus.FAILED,
        FulfillmentStatus.CANCELLED
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fulfillment Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                val (badgeColor, badgeText) = when (fulfillmentStatus) {
                    FulfillmentStatus.PENDING -> Color(0xFFF57C00) to "Pending"
                    FulfillmentStatus.PROCESSING -> Color(0xFF1976D2) to "Processing"
                    FulfillmentStatus.DELIVERED -> Color(0xFF2E7D32) to "Delivered"
                    FulfillmentStatus.FAILED -> Color(0xFFC62828) to "Failed"
                    FulfillmentStatus.CANCELLED -> Color(0xFFC62828) to "Cancelled"
                    FulfillmentStatus.UNKNOWN -> Color.Gray to "Unknown"
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            if (isTerminal) {
                Text(
                    text = "This order is in a terminal state (${fulfillmentStatus.name}) and cannot be modified.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (canUpdate) {
                Text(
                    text = "Update Fulfillment Status:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (fulfillmentStatus == FulfillmentStatus.PENDING) {
                        OutlinedButton(
                            onClick = { onUpdateStatus(FulfillmentStatus.PROCESSING) },
                            enabled = !isUpdating,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark Processing")
                        }
                    }

                    ZenithButton(
                        text = "Mark Delivered",
                        onClick = { onUpdateStatus(FulfillmentStatus.DELIVERED) },
                        enabled = !isUpdating,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    text = "Only Managers and Admins can update fulfillment status.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
fun SaleDetailScreenPreview() {
    SaleDetailScreen(
        saleId     = "sale-id",
        businessId = "business-id",
        onBack = { }
    )
}

@Composable
private fun SaleSummaryCard(saleWithItems: SaleWithItems, cashierName: String) {
    val sale = saleWithItems.sale
    Card(
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailRow("Date",
                Instant.parse(sale.soldAt).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")))
            DetailRow("Cashier", cashierName)
            DetailRow("Payment", sale.paymentMethod.name.lowercase()
                .replaceFirstChar { it.uppercase() })
            if (sale.subtotal != sale.totalAmount) {
                DetailRow("Subtotal", "₦${sale.subtotal.formatAmount()}")
                DetailRow("Discount", "-₦${sale.discountAmount.formatAmount()}")
            }
            DetailRow("Total", "₦${sale.totalAmount.formatAmount()}", bold = true)
            if (sale.amountPaid > 0 && sale.amountPaid != sale.totalAmount) {
                DetailRow("Paid", "₦${sale.amountPaid.formatAmount()}")
            }
            if (sale.changeAmount > 0) {
                DetailRow("Change", "₦${sale.changeAmount.formatAmount()}")
            }
            if (sale.debtAmount > 0) {
                DetailRow("Outstanding", "₦${sale.debtAmount.formatAmount()}",
                    valueColor = Color(0xFFD32F2F))
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    bold: Boolean = false,
    valueColor: Color = Color.Unspecified,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value,
            style      = if (bold) MaterialTheme.typography.bodyMedium
            else MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color      = valueColor)
    }
}

@Composable
private fun SaleItemRow(item: SaleItemEntity) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.productName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium)
            Text("${item.variantSku} × ${Util.formatReceiptQuantity(item.quantity, UnitType.fromString(item.unitType))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("₦${item.totalPrice.formatAmount()}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium)
    }
}
