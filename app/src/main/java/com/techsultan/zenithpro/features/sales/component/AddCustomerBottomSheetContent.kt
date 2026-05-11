package com.techsultan.zenithpro.features.sales.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.features.customer.presentation.CustomerTextField
import com.techsultan.zenithpro.features.customer.presentation.viewmodel.AddEditCustomerViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerBottomSheetContent(
    prefilledPhone: String = "",
    viewModel: AddEditCustomerViewModel = koinViewModel(),
    onSaved: () -> Unit
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(prefilledPhone) {

        if (prefilledPhone.isNotBlank() && state.phone.isBlank()) {
            viewModel.onPhoneChanged(prefilledPhone)
        }
    }

    LaunchedEffect(Unit) {

        viewModel.events.collect { event ->

            when (event) {

                is AddEditCustomerViewModel.AddEditCustomerEvent.Saved -> {
                    onSaved()
                }

                is AddEditCustomerViewModel.AddEditCustomerEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        containerColor = Color.Transparent
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Add Customer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onSaved
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            CustomerTextField(
                value = state.firstName,
                onValueChange = viewModel::onFirstNameChanged,
                label = "First name *",
                error = state.firstNameError,
                keyboardType = KeyboardType.Text
            )

            CustomerTextField(
                value = state.lastName,
                onValueChange = viewModel::onLastNameChanged,
                label = "Last name",
                keyboardType = KeyboardType.Text
            )

            CustomerTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChanged,
                label = "Phone number",
                keyboardType = KeyboardType.Phone
            )

            CustomerTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChanged,
                label = "Email",
                keyboardType = KeyboardType.Email
            )

            CustomerTextField(
                value = state.address,
                onValueChange = viewModel::onAddressChanged,
                label = "Address",
                keyboardType = KeyboardType.Text
            )

            CustomerTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChanged,
                label = "Notes",
                keyboardType = KeyboardType.Text,
                minLines = 3
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    viewModel.save()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !state.isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {

                if (state.isLoading) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )

                } else {

                    Text(
                        text = "Save Customer",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}