package com.techsultan.zenithpro.features.settings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.components.CustomTextField
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStaffScreen(
    viewModel: CreateStaffViewModel = koinViewModel(),
    onCreated: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost  = remember { SnackbarHostState() }
    var showPassword  by remember { mutableStateOf(false) }
    var showRoleMenu  by remember { mutableStateOf(false) }
    var showBranchMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CreateStaffViewModel.CreateStaffEvent.Created -> {
                    snackbarHost.showSnackbar(
                        "${event.staff.firstName} added — they can now log in"
                    )
                    onCreated()
                }
                is CreateStaffViewModel.CreateStaffEvent.ShowError ->
                    snackbarHost.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            ZenithTopAppBar(
                title = "Add staff member",
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Surface(
                shape  = RoundedCornerShape(10.dp),
                color  = Color(0xFF1976D2).copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint     = Color(0xFF1976D2),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "The staff member will be asked to change their " +
                                "password on first login.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1976D2)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CustomTextField(
                    value = state.firstName,
                    onValueChange = viewModel::onFirstNameChanged,
                    label = "First name *",
                    keyboardType = KeyboardType.Text,
                    placeholder = "Enter first name",
                    modifier = Modifier.weight(1f),
                    error = state.firstNameError,
                )
                CustomTextField(
                    value  = state.lastName,
                    onValueChange = viewModel::onLastNameChanged,
                    label = "Last name *",
                    keyboardType = KeyboardType.Text,
                    placeholder = "Enter last name",
                    modifier = Modifier.weight(1f),
                )
            }

            CustomTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChanged,
                label = "Phone number",
                keyboardType = KeyboardType.Phone,
                placeholder = "Enter phone number",
                modifier = Modifier,
            )

            CustomTextField(
                value  = state.email,
                onValueChange = viewModel::onEmailChanged,
                label = "Email address",
                keyboardType = KeyboardType.Email,
                placeholder = "Enter email address",
                modifier = Modifier,
                error = state.emailError,
            )
            OutlinedTextField(
                value         = state.password,
                onValueChange = viewModel::onPasswordChanged,
                label         = { Text("Temporary password *") },
                modifier      = Modifier.fillMaxWidth(),
                isError       = state.passwordError != null,
                supportingText = state.passwordError?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                } ?: { Text("Min. 6 characters",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall) },
                visualTransformation = if (showPassword)
                    VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF9FAFB),
                    focusedContainerColor = Color(0xFFF9FAFB),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
            )

            Text(
                "Role",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "STAFF"   to "Staff",
                    "MANAGER" to "Manager",
                    "ADMIN"   to "Admin"
                ).forEach { (value, label) ->
                    val selected = state.selectedRole == value
                    Surface(
                        shape    = RoundedCornerShape(10.dp),
                        color    = if (selected)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.onRoleChanged(value) }
                    ) {
                        Column(
                            modifier            = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (value) {
                                    "ADMIN"   -> Icons.Default.AdminPanelSettings
                                    "MANAGER" -> Icons.Default.SupervisorAccount
                                    else      -> Icons.Default.Person
                                },
                                contentDescription = null,
                                tint     = if (selected)
                                    MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected)
                                    MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = when (state.selectedRole) {
                        "ADMIN"   -> "Full access — can manage staff, settings, all branches and reports"
                        "MANAGER" -> "Can manage products, sales, customers and view reports"
                        else      -> "Can process sales and view their own transactions"
                    },
                    modifier = Modifier.padding(10.dp),
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.branches.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded         = showBranchMenu,
                    onExpandedChange = { showBranchMenu = it }
                ) {
                    OutlinedTextField(
                        value = state.branches
                            .firstOrNull { it.id == state.selectedBranchId }
                            ?.name ?: "All branches (no restriction)",
                        onValueChange = {},
                        label = { Text("Assign to branch") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(showBranchMenu)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF9FAFB),
                            focusedContainerColor = Color(0xFFF9FAFB),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                    )
                    ExposedDropdownMenu(
                        expanded         = showBranchMenu,
                        onDismissRequest = { showBranchMenu = false }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("All branches")
                                    Text(
                                        "No branch restriction",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                viewModel.onBranchChanged(null)
                                showBranchMenu = false
                            }
                        )
                        HorizontalDivider()
                        state.branches
                            .filter { it.isActive }
                            .forEach { branch ->
                                DropdownMenuItem(
                                    text    = {
                                        Column {
                                            Text(branch.name)
                                            branch.address?.let {
                                                Text(
                                                    it,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.onBranchChanged(branch.id)
                                        showBranchMenu = false
                                    }
                                )
                            }
                    }
                }
            }

            state.error?.let { error ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(12.dp),
                        style  = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = viewModel::createStaff,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled  = !state.isLoading,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Create account",
                        style  = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}