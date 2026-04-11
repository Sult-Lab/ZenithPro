package com.techsultan.zenithpro.features.settings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.core.util.Util.trimOrNull
import com.techsultan.zenithpro.features.settings.data.local.BusinessSettingsEntity
import org.koin.androidx.compose.koinViewModel
import java.time.Instant

@Composable
fun BusinessInformationScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit
){

    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsViewModel.SettingsEvent.Saved ->
                    snackbarHost.showSnackbar("Settings saved")
                is SettingsViewModel.SettingsEvent.RoleUpdated ->
                    snackbarHost.showSnackbar("Role updated")
                is SettingsViewModel.SettingsEvent.ShowError ->
                    snackbarHost.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            ZenithTopAppBar(
                title = "Business Information",
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
    ){ paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ){
            item {
                state.settings?.let { settings ->
                    BusinessSettingsSection(
                        settings = settings,
                        onSave   = { viewModel.updateSettings(it) },
                        isSaving = state.isSaving
                    )
                }
            }
        }
    }
}

@Composable
private fun BusinessSettingsSection(
    settings: BusinessSettingsEntity,
    onSave: (BusinessSettingsEntity) -> Unit,
    isSaving: Boolean,
) {
    var taxRate by remember { mutableStateOf(settings.taxRate.toString()) }
    var allowNegStock by remember { mutableStateOf(settings.allowNegativeStock) }
    var requireCustomer by remember { mutableStateOf(settings.requireCustomerSale) }
    var lowStockThreshold by remember { mutableStateOf(settings.lowStockThreshold.toString()) }
    var receiptFooter by remember { mutableStateOf(settings.receiptFooter ?: "") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsSectionHeader(title = "BUSINESS SETTINGS")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = taxRate,
                    onValueChange = { taxRate = it },
                    label = { Text("Tax rate (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = lowStockThreshold,
                    onValueChange = { lowStockThreshold = it },
                    label = { Text("Default low stock threshold") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape  = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = receiptFooter,
                    onValueChange = { receiptFooter = it },
                    label = { Text("Receipt footer message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp)
                )

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                SettingsToggleRow(
                    title = "Allow negative stock",
                    subtitle = "Sell even when stock is zero",
                    checked = allowNegStock,
                    onCheckedChange = { allowNegStock = it }
                )

                SettingsToggleRow(
                    title = "Require customer for sales",
                    subtitle = "Block sales without a customer",
                    checked = requireCustomer,
                    onCheckedChange = { requireCustomer = it }
                )

                Button(
                    onClick  = {
                        onSave(
                            settings.copy(
                                taxRate = taxRate.toDoubleOrNull() ?: 0.0,
                                allowNegativeStock  = allowNegStock,
                                requireCustomerSale = requireCustomer,
                                lowStockThreshold  = lowStockThreshold.toIntOrNull() ?: 5,
                                receiptFooter = receiptFooter.trimOrNull(),
                                updatedAt = Instant.now().toString()
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled  = !isSaving,
                    shape  = RoundedCornerShape(10.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            color       = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}