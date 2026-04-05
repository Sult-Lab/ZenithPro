package com.techsultan.zenithpro.features.settings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.core.util.Util.trimOrNull
import com.techsultan.zenithpro.features.settings.data.local.BusinessSettingsEntity
import com.techsultan.zenithpro.features.settings.data.remote.StaffMember
import org.koin.androidx.compose.koinViewModel
import java.time.Instant

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit,
    onAccountSettings: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onAbout: () -> Unit = {},
    onLogout: () -> Unit = {},
) {

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
                title = "Settings",
                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(
//                            imageVector        = Icons.Default.ArrowBack,
//                            contentDescription = "Back",
//                            tint               = MaterialTheme.colorScheme.onSurface
//                        )
//                    }
                },
                actions = {}
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            if (viewModel.isAdmin) {
                item {
                    SettingsSectionHeader(title = "ADMIN")
                }

                // Business settings form (inline)
                item {
                    state.settings?.let { settings ->
                        BusinessSettingsSection(
                            settings = settings,
                            onSave   = { viewModel.updateSettings(it) },
                            isSaving = state.isSaving
                        )
                    }
                }

                if (state.staffList.isNotEmpty()) {
                    item {
                        Text(
                            text = "Staff management",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(state.staffList, key = { it.id }) { staff ->
                        StaffCard(
                            staff          = staff,
                            isCurrentUser  = staff.id == viewModel.currentUserId,
                            onRoleChange   = { role ->
                                viewModel.updateStaffRole(staff.id, role)
                            },
                            onStatusToggle = { status ->
                                viewModel.updateStaffStatus(staff.id, status)
                            }
                        )
                    }
                }
            }

            item {
                SettingsNavSection(
                    title = "GENERAL",
                    items = listOf(
                        SettingsNavItem(
                            title  = "Account settings",
                            icon   = Icons.Default.Person,
                            onClick = onAccountSettings
                        ),
                        SettingsNavItem(
                            title  = "Notifications",
                            icon   = Icons.Default.Notifications,
                            onClick = onNotifications
                        ),
                        SettingsNavItem(
                            title  = "About app",
                            icon   = Icons.Default.Info,
                            onClick = onAbout
                        )
                    )
                )
            }


            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedButton(
                        onClick  = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape    = RoundedCornerShape(10.dp),
                        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Logout,
                            contentDescription = null,
                            modifier           = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text       = "Log out",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text  = "Version 1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}



@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text  = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}



data class SettingsNavItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun SettingsNavSection(
    title: String,
    items: List<SettingsNavItem>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsSectionHeader(title = title)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(12.dp),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = item.onClick)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = item.icon,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier           = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text       = item.title,
                            style      = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.onSurface,
                            modifier   = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector        = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    if (index < items.lastIndex) {
                        HorizontalDivider(
                            modifier  = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
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
    var taxRate           by remember { mutableStateOf(settings.taxRate.toString()) }
    var allowNegStock     by remember { mutableStateOf(settings.allowNegativeStock) }
    var requireCustomer   by remember { mutableStateOf(settings.requireCustomerSale) }
    var lowStockThreshold by remember { mutableStateOf(settings.lowStockThreshold.toString()) }
    var receiptFooter     by remember { mutableStateOf(settings.receiptFooter ?: "") }

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

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StaffCard(
    staff: StaffMember,
    isCurrentUser: Boolean,
    onRoleChange: (String) -> Unit,
    onStatusToggle: (String) -> Unit,
) {
    var showRoleMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = staff.firstName.first().uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(12.dp))

            // Name + email
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = staff.fullName,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isCurrentUser) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "You",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                staff.email?.let {
                    Text(
                        text  = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isCurrentUser) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { showRoleMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text  = staff.role,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showRoleMenu,
                            onDismissRequest = { showRoleMenu = false }
                        ) {
                            listOf("ADMIN", "MANAGER", "STAFF").forEach { role ->
                                DropdownMenuItem(
                                    text    = {
                                        Text(
                                            text = role.lowercase().replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    onClick = { onRoleChange(role); showRoleMenu = false },
                                    leadingIcon = if (staff.role == role) {
                                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text  = if (staff.status == "ACTIVE") "Active" else "Inactive",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (staff.status == "ACTIVE")
                                Color(0xFF388E3C)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = staff.status == "ACTIVE",
                            onCheckedChange = { active ->
                                onStatusToggle(if (active) "ACTIVE" else "INACTIVE")
                            },
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                }
            }
        }
    }
}
