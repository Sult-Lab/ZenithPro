package com.techsultan.zenithpro.features.settings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.components.CustomTextField
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import com.techsultan.zenithpro.features.settings.data.remote.StaffMember
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditStaffScreen(
    staff: StaffMember,
    currentUserRole: String,
    viewModel: EditStaffViewModel = koinViewModel(),
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(staff) { viewModel.loadStaff(staff) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost  = remember { SnackbarHostState() }
    var showRoleSheet by remember { mutableStateOf(false) }
    var showBranchSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditStaffViewModel.EditStaffEvent.Saved -> {
                    snackbarHost.showSnackbar("Staff updated successfully")
                    onSaved()
                }
                is EditStaffViewModel.EditStaffEvent.ShowError ->
                    snackbarHost.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            ZenithTopAppBar(
                title = "Edit staff",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = staff.firstName.first().uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column {
                    Text(staff.fullName,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text(staff.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider()

            Text("Personal information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color  = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CustomTextField(
                    value = state.firstName,
                    onValueChange = viewModel::onFirstNameChanged,
                    label = "First name *",
                    keyboardType = KeyboardType.Text,
                    placeholder = "Enter first name",
                    modifier = Modifier.weight(1f),
                    error = state.firstNameError
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
            )

            HorizontalDivider()

            Text("Role & assignment",
                style  = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color  = MaterialTheme.colorScheme.onSurfaceVariant)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRoleSheet = true },
                shape  = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier          = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        when (state.role) {
                            "ADMIN"   -> Icons.Default.AdminPanelSettings
                            "MANAGER" -> Icons.Default.SupervisorAccount
                            else      -> Icons.Default.Person
                        },
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Role",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.role.lowercase().replaceFirstChar { it.uppercase() },
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }


            if (state.branches.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBranchSheet = true },
                    shape  = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier  = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Branch assignment",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = state.branches.firstOrNull { it.id == state.branchId }?.name
                                    ?: "No branch assigned",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (state.branchId == null)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            Text("Account status",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (state.status == "ACTIVE") "Active" else "Inactive",
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color      = if (state.status == "ACTIVE")
                            Color(0xFF388E3C)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (state.status == "ACTIVE")
                            "Staff can log in and process sales"
                        else
                            "Staff cannot log in",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.status == "ACTIVE",
                    onCheckedChange = { active ->
                        viewModel.onStatusChanged(if (active) "ACTIVE" else "INACTIVE")
                    }
                )
            }

            state.error?.let { error ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = !state.isLoading,
                shape    = RoundedCornerShape(12.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Save changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showRoleSheet) {
        RolePickerSheet(
            currentRole     = state.role,
            currentUserRole = currentUserRole,
            onSelect        = {
                viewModel.onRoleChanged(it)
                showRoleSheet = false
            },
            onDismiss       = { showRoleSheet = false }
        )
    }

    if (showBranchSheet) {
        BranchAssignmentSheet(
            branches       = state.branches,
            selectedBranchId = state.branchId,
            onSelect       = {
                viewModel.onBranchChanged(it)
                showBranchSheet = false
            },
            onDismiss = { showBranchSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RolePickerSheet(
    currentRole: String,
    currentUserRole: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val roles = buildList {
        add(Triple(
            "STAFF",
            Icons.Default.Person,
            "Can process sales, view own transactions"
        ))
        add(Triple(
            "MANAGER",
            Icons.Default.SupervisorAccount,
            "Can manage products, view all sales and reports"
        ))
        // Only admins can assign admin role
        if (currentUserRole == "ADMIN") {
            add(Triple(
                "ADMIN",
                Icons.Default.AdminPanelSettings,
                "Full access including settings and staff management"
            ))
        }
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
                text = "Select role",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)

            roles.forEach { (role, icon, description) ->
                val isSelected = currentRole == role
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(role) },
                    shape  = RoundedCornerShape(12.dp),
                    color  = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null,
                                tint     = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                role.lowercase().replaceFirstChar { it.uppercase() },
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BranchAssignmentSheet(
    branches: List<BranchEntity>,
    selectedBranchId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Assign to branch",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Staff will only see sales and data from their assigned branch",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(null) },
                shape  = RoundedCornerShape(12.dp),
                color  = if (selectedBranchId == null)
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    if (selectedBranchId == null) 2.dp else 1.dp,
                    if (selectedBranchId == null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier          = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "No branch restriction",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Staff can access all branches",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selectedBranchId == null) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            HorizontalDivider()

            branches.forEach { branch ->
                val isSelected = branch.id == selectedBranchId
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(branch.id) },
                    shape  = RoundedCornerShape(12.dp),
                    color  = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier          = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Store, null,
                                tint     = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(branch.name,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold)
                            branch.address?.let {
                                Text(it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}