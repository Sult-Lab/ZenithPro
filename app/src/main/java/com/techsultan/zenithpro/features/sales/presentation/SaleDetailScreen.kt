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
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
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
        val sale = state.saleWithItems?.sale

        if (sale == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier   = Modifier.fillMaxSize().padding(padding),
            contentPadding  = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Awaiting transfer panel (only when AWAITING_PAYMENT) ──
            if (sale.paymentStatus == "AWAITING_PAYMENT") {
                item {
                    AwaitingTransferPanel(
                        sale        = sale,
                        isPolling   = state.isPolling
                    )
                }
            } else if (sale.paymentMethod == PaymentMethod.TRANSFER &&
                sale.paymentStatus == "COMPLETED") {
                item { TransferConfirmedBanner(sale = sale) }
            }

            // ── Sale summary ──────────────────────────────────────────
            item {
                SaleSummaryCard(saleWithItems = state.saleWithItems!!)
            }

            // ── Items ─────────────────────────────────────────────────
            item {
                Text(
                    text = "Items",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
            }
            items(
                items = state.saleWithItems!!.items) { item ->
                SaleItemRow(item = item)
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
private fun AwaitingTransferPanel(
    sale: SaleEntity,
    isPolling: Boolean,
) {
    val context = LocalContext.current

    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f))
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status header
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isPolling) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        color       = Color(0xFFFFB300),
                        strokeWidth = 2.dp
                    )
                } else {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue  = 0.4f,
                        targetValue   = 1f,
                        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                        label         = "alpha"
                    )
                    Box(
                        Modifier.size(10.dp)
                            .background(Color(0xFFFFB300).copy(alpha = alpha), CircleShape)
                    )
                }
                Text("Waiting for bank transfer",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFFFFB300))
            }

            Text("₦${sale.totalAmount.formatAmount()}",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface)

            HorizontalDivider(color = Color(0xFFFFB300).copy(alpha = 0.2f))

            // Virtual account details
            sale.virtualAccountNumber?.let { accNum ->
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TransferDetailRow("Bank", sale.virtualAccountBank ?: "—")
                    TransferDetailRow(
                        label    = "Account number",
                        value    = accNum,
                        copyable = true,
                        context  = context
                    )
                    TransferDetailRow("Account name", sale.virtualAccountName ?: "—")
                }
            }

            // Reference — most prominent
            sale.paymentReference?.let { ref ->
                Surface(
                    shape  = RoundedCornerShape(10.dp),
                    color  = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier            = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Include in transfer narration",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                .copy(alpha = 0.7f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(ref,
                                style         = MaterialTheme.typography.headlineSmall,
                                fontWeight    = FontWeight.ExtraBold,
                                color         = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp)
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(
                                        Context.CLIPBOARD_SERVICE
                                    ) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(
                                        android.content.ClipData.newPlainText("ref", ref)
                                    )
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, null,
                                    modifier = Modifier.size(16.dp),
                                    tint     = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            Text(
                text = "Background monitoring is active. You'll be notified when payment is received.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TransferConfirmedBanner(sale: SaleEntity) {
    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = Color(0xFF388E3C).copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color(0xFF388E3C).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.CheckCircle, null,
                tint     = Color(0xFF388E3C),
                modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Transfer confirmed",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF388E3C))
                sale.paymentConfirmedAt?.let {
                    Text(
                        Instant.parse(it)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF388E3C).copy(alpha = 0.7f)
                    )
                }
            }
            sale.paymentReference?.let {
                Text(it,
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF388E3C))
            }
        }
    }
}

@Composable
private fun TransferDetailRow(
    label: String,
    value: String,
    copyable: Boolean = false,
    context: Context? = null,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
        }
        if (copyable && context != null) {
            IconButton(
                onClick  = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText("value", value)
                    )
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.ContentCopy, null,
                    modifier = Modifier.size(14.dp),
                    tint     = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SaleSummaryCard(saleWithItems: SaleWithItems) {
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
            Text("${item.variantSku} × ${item.quantity}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("₦${item.totalPrice.formatAmount()}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium)
    }
}