package com.techsultan.zenithpro.features.customer.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.components.CustomTextField
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.features.customer.data.local.CustomerEntity
import com.techsultan.zenithpro.features.customer.presentation.viewmodel.AddEditCustomerViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCustomerScreen(
    existingCustomer: CustomerEntity? = null,
    viewModel: AddEditCustomerViewModel = koinViewModel(),
    onSaved: (String) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(existingCustomer) {
        existingCustomer?.let { viewModel.loadCustomer(it) }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddEditCustomerViewModel.AddEditCustomerEvent.Saved ->
                    onSaved(event.customerId)

                is AddEditCustomerViewModel.AddEditCustomerEvent.ShowError ->
                    snackbarHost.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            ZenithTopAppBar(
                title = if (state.isEditMode) "Edit customer" else "New customer",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "back")
                    }
                },
                actions = {

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
            CustomTextField(
                value = state.firstName,
                onValueChange = viewModel::onFirstNameChanged,
                label = "First name",
                error = state.firstNameError,
                keyboardType = KeyboardType.Text,
                placeholder = "Enter first name"
            )
            CustomTextField(
                value = state.lastName,
                onValueChange = viewModel::onLastNameChanged,
                label = "Last name",
                keyboardType = KeyboardType.Text,
                placeholder = "Enter last name"
            )
            CustomTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChanged,
                label = "Phone number",
                keyboardType = KeyboardType.Phone,
                placeholder = "Enter phone number"
            )
            CustomTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChanged,
                label = "Email Address",
                keyboardType = KeyboardType.Email,
                placeholder = "Enter email address"
            )
            CustomTextField(
                value = state.address,
                onValueChange = viewModel::onAddressChanged,
                label = "Physical Address",
                keyboardType = KeyboardType.Text,
                placeholder = "Enter physical address"
            )
//            CustomTextField(
//                value = state.address,
//                onValueChange = viewModel::onAddressChanged,
//                label = "Initial debt",
//                keyboardType = KeyboardType.Number,
//                placeholder = "Enter customer initial debt"
//            )
            CustomTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChanged,
                label = "Notes",
                keyboardType = KeyboardType.Text,
                minLines = 3,
                placeholder = "Note"
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isLoading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00C853)
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (state.isEditMode) "Update customer" else "Save customer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}