package com.techsultan.zenithpro.features.sales.presentation

import android.content.Context
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.features.sales.formatAmount
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AwaitingTransferScreen(
    saleId: String,
    totalAmount: Long,
    paymentReference: String,
    virtualAccountNumber: String,
    virtualAccountBank: String,
    virtualAccountName: String,
    businessId: String,
    onConfirmed: () -> Unit,   // called when webhook confirms
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var isConfirmed  by remember { mutableStateOf(false) }
    var pollCount    by remember { mutableStateOf(0) }

    // Poll every 5 seconds for up to 5 minutes
    LaunchedEffect(saleId) {
        while (!isConfirmed && pollCount < 60) {
            delay(5_000.milliseconds)
            pollCount++
            // Check if webhook has confirmed this sale
            // Simple Supabase query — no edge function needed
            // This is handled by SaleStatusPoller below
        }
    }

    Scaffold(
        topBar = {
            ZenithTopAppBar(
                title = "Waiting for payment",
                navigationIcon = {
                    IconButton(onClick = { onCancel() }) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier  = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Amount
            Text(
                text = "₦${totalAmount.formatAmount()}",
                style  = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text(
                text = "Awaiting bank transfer",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Pulsing indicator
            TransferWaitingIndicator()

            // Virtual account card
            Card(
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier            = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Transfer to this account",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)

                    AccountDetailRow(
                        label = "Bank",
                        value = virtualAccountBank
                    )
                    AccountDetailRow(
                        label = "Account number",
                        value = virtualAccountNumber,
                        copyable = true,
                        context  = context
                    )
                    AccountDetailRow(
                        label = "Account name",
                        value = virtualAccountName
                    )

                    HorizontalDivider()

                    // Reference — most important field
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Include this reference in narration",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = paymentReference,
                            style      = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color      = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info, null,
                                modifier = Modifier.size(12.dp),
                                tint     = MaterialTheme.colorScheme.onPrimaryContainer
                                    .copy(alpha = 0.6f))
                            Text("e.g. \"Payment for goods $paymentReference\"",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                    .copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // Status
            Surface(
                shape  = RoundedCornerShape(10.dp),
                color  = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Waiting for transfer confirmation...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.weight(1f))

            Text("Payment will be confirmed automatically once received.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }

    // Poll for confirmation
//    SaleStatusPoller(
//        saleId      = saleId,
//        businessId  = businessId,
//        onConfirmed = { isConfirmed = true; onConfirmed() }
//    )
}

@Composable
private fun AccountDetailRow(
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
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
        }
        if (copyable && context != null) {
            IconButton(
                onClick  = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText("account", value)
                    )
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy, null,
                    modifier = Modifier.size(16.dp),
                    tint     = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun TransferWaitingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue   = 0.9f,
        targetValue    = 1.1f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Box(
        modifier         = Modifier
            .size(80.dp)
            .scale(scale)
            .background(Color(0xFF1976D2).copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.AccountBalance, null,
            modifier = Modifier.size(36.dp),
            tint     = Color(0xFF1976D2))
    }
}