package com.techsultan.zenithpro.features.branch.presentation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.util.Util.trimOrNull
import com.techsultan.zenithpro.features.branch.data.local.BranchEntity
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchScreen(
    viewModel: BranchViewModel,
    onBack: () -> Unit,
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost  = remember { SnackbarHostState() }
    var showAddSheet  by remember { mutableStateOf(false) }
    var editingBranch by remember { mutableStateOf<BranchEntity?>(null) }
    val isAdmin = viewModel.isAdmin

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BranchViewModel.BranchEvent.Saved ->
                    snackbarHost.showSnackbar("Branch saved")
                is BranchViewModel.BranchEvent.Deleted ->
                    snackbarHost.showSnackbar("Branch deleted")
                is BranchViewModel.BranchEvent.ShowError ->
                    snackbarHost.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Branches") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { showAddSheet = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add branch")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier  = Modifier.fillMaxSize().padding(padding),
            contentPadding  = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.branches.isEmpty() && !state.isLoading) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No branches yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(state.branches, key = { it.id }) { branch ->
                BranchCard(
                    branch = branch,
                    isAdmin = isAdmin,
                    onEdit = { editingBranch = branch; showAddSheet = true },
                    onDelete = { viewModel.deleteBranch(branch.id) },
                    onToggle = { viewModel.toggleActive(branch.id, !branch.isActive) }
                )
            }
        }
    }

    if (showAddSheet) {
        AddEditBranchSheet(
            existing   = editingBranch,
            onSave     = { name, address, phone ->
                viewModel.upsertBranch(
                    editingBranch?.id, name, address, phone,
                )
                showAddSheet  = false
                editingBranch = null
            },
            onDismiss  = { showAddSheet = false; editingBranch = null }
        )
    }
}

@Composable
private fun BranchCard(
    branch: BranchEntity,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        shape  = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (branch.isActive) Color(0xFF1976D2).copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    tint = if (branch.isActive) Color(0xFF1976D2)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        branch.name,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (branch.isActive) Color(0xFF388E3C).copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            if (branch.isActive) "Active" else "Inactive",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = if (branch.isActive) Color(0xFF388E3C)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                branch.address?.let {
                    Text(it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                branch.phone?.let {
                    Text(it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (isAdmin) {
                Row {
                    Switch(
                        checked  = branch.isActive,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.scale(0.8f)
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, null,
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text("Delete branch") },
            text    = { Text("Delete \"${branch.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors  = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditBranchSheet(
    existing: BranchEntity?,
    onSave: (String, String?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name    by remember { mutableStateOf(existing?.name ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var phone   by remember { mutableStateOf(existing?.phone ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (existing != null) "Edit branch" else "New branch",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Branch name *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value = address, onValueChange = { address = it },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(10.dp)
            )
            Button(
                onClick  = { if (name.isNotBlank()) onSave(name, address.trimOrNull(), phone.trimOrNull()) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
            ) { Text("Save branch", fontWeight = FontWeight.Bold) }
        }
    }
}