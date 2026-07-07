package com.techsultan.zenithpro.features.settings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.core.util.NigerianBanks
import com.techsultan.zenithpro.features.settings.data.local.TerminalEntity
import com.techsultan.zenithpro.features.settings.presentation.viewmodel.PaymentSettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PaymentSettingsScreen(
    viewModel: PaymentSettingsViewModel = koinViewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost  = remember { SnackbarHostState() }
    var editingTerminal by remember { mutableStateOf<TerminalEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PaymentSettingsViewModel.PaymentSettingsEvent.SweepSaved ->
                    snackbarHost.showSnackbar("Bank account saved")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            ZenithTopAppBar(
                title = "Payment settings",
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {}
            )
        }
    ) { padding ->
        LazyColumn(
            modifier  = Modifier.fillMaxSize().padding(padding),
            contentPadding  = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Test mode ───────────────────────────────────────────
            item {
                TestModeCard(
                    enabled   = state.testModeEnabled,
                    onToggle  = viewModel::onTestModeToggled
                )
            }

            // ── Payment methods header ──────────────────────────────
            item {
                Text("Payment methods",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // ── Transfer card per terminal ──────────────────────────
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.terminals.isEmpty()) {
                item { NoTerminalCard() }
            } else {
                items(state.terminals, key = { it.id }) { terminal ->
                    TerminalPaymentCard(
                        terminal         = terminal,
                        transferEnabled  = state.transferEnabled,
                        onTransferToggle = viewModel::onTransferToggled,
                        onEditSweep      = { editingTerminal = terminal }
                    )
                }
            }

            // ── Sweep account setup banner ──────────────────────────
            // Show when any terminal has virtual account but no sweep account
            if (state.terminals.any {
                    it.isNombaOnboarded && it.nombaSweepAccountNumber == null }) {
                item { SweepSetupBanner() }
            }

            // ── Nomba info card ─────────────────────────────────────
            item { NombaInfoCard() }

            // ── Subscription notice ─────────────────────────────────
            item { SubscriptionNoticeCard() }
        }
    }

    // Sweep account bottom sheet
    editingTerminal?.let { terminal ->
        SweepAccountSheet(
            terminal    = terminal,
            isSaving    = state.isSaving,
            errorMessage = state.sweepError,
            onSave      = { bankCode, accountNumber, accountName ->
                viewModel.saveSweepAccount(
                    terminal.id, bankCode, accountNumber, accountName
                )
                editingTerminal = null
            },
            onDismiss   = { editingTerminal = null }
        )
    }
}

@Composable
private fun TestModeCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled)
                Color(0xFFFFF8E1)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (enabled)
            BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f))
        else null
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (enabled) Color(0xFFFFB300).copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Science, null,
                    tint     = if (enabled) Color(0xFFFFB300)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Test mode",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold)
                Text("Simulate transactions without real money",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors  = SwitchDefaults.colors(
                    checkedThumbColor  = Color.White,
                    checkedTrackColor  = Color(0xFFFFB300)
                )
            )
        }
    }
}

@Composable
private fun TerminalPaymentCard(
    terminal: TerminalEntity,
    transferEnabled: Boolean,
    onTransferToggle: (Boolean) -> Unit,
    onEditSweep: () -> Unit,
) {
    Card(
        shape  = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Color(0xFF1976D2).copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint     = Color(0xFF1976D2),
                        modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bank Transfer (Nomba)",
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(terminal.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = transferEnabled,
                    onCheckedChange = onTransferToggle
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            Column(
                modifier            = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status badge
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (terminal.isNombaOnboarded) Color(0xFF388E3C)
                                else Color(0xFFFFB300),
                                CircleShape
                            )
                    )
                    Text(
                        text = if (terminal.isNombaOnboarded) "Active" else "Setup required",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (terminal.isNombaOnboarded) Color(0xFF388E3C)
                        else Color(0xFFFFB300),
                        fontWeight = FontWeight.Medium
                    )
                }

                if (terminal.isNombaOnboarded) {
                    // Virtual account row
                    VirtualAccountRow(terminal = terminal)

                    // Sweep destination row
                    SweepAccountRow(
                        terminal    = terminal,
                        onEdit      = onEditSweep
                    )
                } else {
                    // Not yet provisioned
                    Surface(
                        shape  = RoundedCornerShape(8.dp),
                        color  = Color(0xFFFFB300).copy(alpha = 0.08f),
                        border = BorderStroke(
                            1.dp, Color(0xFFFFB300).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier          = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, null,
                                tint     = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp))
                            Text(
                                "Virtual account not yet provisioned for this terminal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFB300).copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VirtualAccountRow(terminal: TerminalEntity) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CreditCard,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Virtual account",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "${terminal.nombaVirtualAccountBank ?: "Wema Bank"} · ${terminal.nombaVirtualAccountNumber ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            terminal.nombaVirtualAccountName?.let {
                Text(it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // Copy button
        IconButton(
            onClick  = { /* copy to clipboard */ },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.ContentCopy, null,
                modifier = Modifier.size(16.dp),
                tint     = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SweepAccountRow(
    terminal: TerminalEntity,
    onEdit: () -> Unit,
) {
    Row(
        modifier  = Modifier.fillMaxWidth(),
        verticalAlignment  = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Sweeps to",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (terminal.nombaSweepAccountNumber != null) {
                Text(
                    text = "${terminal.nombaSweepAccountName ?: "Your bank"} · ${terminal.maskedSweepAccount}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "Not set — add your bank account",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD32F2F)
                )
            }
        }
        IconButton(
            onClick  = onEdit,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit bank account",
                modifier = Modifier.size(16.dp),
                tint  = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SweepSetupBanner() {
    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = Color(0xFFD32F2F).copy(alpha = 0.07f),
        border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.25f))
    ) {
        Row(
            modifier  = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(18.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Add your bank account",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD32F2F))
                Text(
                    text = "Without a sweep account, transfers from customers cannot " +
                            "be routed to you. Tap the edit icon above to add your bank details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD32F2F).copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun NoTerminalCard() {
    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PointOfSale,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            Text(
                text = "No terminals registered",
                style  = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "Terminals are set up by your ZenithPro account manager. " +
                        "Each terminal gets its own Nomba virtual account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NombaInfoCard() {
    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = Color(0xFF1976D2).copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.2f))
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = null,
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Need help?",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF1976D2))
                Text(
                    "Your virtual account is powered by Nomba.\n" +
                            "All transfers are automatically sent to your registered bank account. " +
                            "Each terminal has its own unique virtual account number.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1976D2).copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun SubscriptionNoticeCard() {
    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint     = Color(0xFF7B1FA2),
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Subscription billing",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold)
                Text("Monthly, quarterly and yearly plans — coming soon.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF7B1FA2).copy(alpha = 0.12f)
            ) {
                Text("Soon",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style    = MaterialTheme.typography.labelSmall,
                    color    = Color(0xFF7B1FA2))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SweepAccountSheet(
    terminal: TerminalEntity,
    isSaving: Boolean,
    errorMessage: String?,
    onSave: (bankCode: String, accountNumber: String, accountName: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var accountNumber  by remember { mutableStateOf(terminal.nombaSweepAccountNumber ?: "") }
    var accountName    by remember { mutableStateOf(terminal.nombaSweepAccountName ?: "") }
    var selectedBank   by remember {
        mutableStateOf(
            NigerianBanks.ALL.firstOrNull { it.code == terminal.nombaSweepBankCode }
                ?: NigerianBanks.ALL.first()
        )
    }
    var showBankPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Bank account for payouts",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Text(
                "Funds from transfers on ${terminal.name} will be " +
                        "automatically swept to this account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Bank selector
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBankPicker = true },
                shape  = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier  = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bank",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(selectedBank.name,
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = accountNumber,
                onValueChange = { if (it.length <= 10) accountNumber = it },
                label = { Text("Account number") },
                modifier  = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape  = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value         = accountName,
                onValueChange = { accountName = it },
                label         = { Text("Account name") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                shape         = RoundedCornerShape(10.dp),
                supportingText = {
                    Text("Enter the name as it appears on your bank account",
                        style = MaterialTheme.typography.labelSmall)
                }
            )

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick  = {
                    onSave(selectedBank.code, accountNumber, accountName)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = !isSaving &&
                        accountNumber.length == 10 &&
                        accountName.isNotBlank(),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Save bank account",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showBankPicker) {
        BankPickerSheet(
            selectedBank = selectedBank,
            onSelect = { selectedBank = it; showBankPicker = false },
            onDismiss = { showBankPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankPickerSheet(
    selectedBank: NigerianBanks.Bank,
    onSelect: (NigerianBanks.Bank) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) NigerianBanks.ALL
        else NigerianBanks.ALL.filter { it.name.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Select bank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search banks...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = filtered, key = { it.code }) { bank ->
                    val isSelected = bank.code == selectedBank.code
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(bank) },
                        shape  = RoundedCornerShape(8.dp),
                        color  = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(bank.name,
                                style    = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, null,
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}