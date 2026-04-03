package com.techsultan.zenithpro.features.expenses.presentation

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import com.techsultan.zenithpro.features.expenses.data.local.ExpenseEntity
import com.techsultan.zenithpro.features.expenses.viewmodel.AddEditExpenseViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(
    businessId: String,
    staffId: String,
    existingExpense: ExpenseEntity? = null,
    viewModel: AddEditExpenseViewModel = koinViewModel(),
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        existingExpense?.let { viewModel.loadForEdit(it) }
        viewModel.loadCategories(businessId)
    }

    val state    by viewModel.state.collectAsStateWithLifecycle()
    val snackbar  = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddEditExpenseViewModel.AddEditExpenseEvent.Saved ->
                    onSaved()
                is AddEditExpenseViewModel.AddEditExpenseEvent.ShowError ->
                    snackbar.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isEditMode) "Edit expense" else "New expense")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
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
            // Title
            OutlinedTextField(
                value         = state.title,
                onValueChange = viewModel::onTitleChanged,
                label         = { Text("Title *") },
                modifier      = Modifier.fillMaxWidth(),
                isError       = state.titleError != null,
                supportingText = state.titleError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                shape         = RoundedCornerShape(10.dp)
            )

            // Amount
            OutlinedTextField(
                value = state.amount,
                onValueChange = viewModel::onAmountChanged,
                label = { Text("Amount *") },
                prefix = { Text("₦") },
                modifier = Modifier.fillMaxWidth(),
                isError = state.amountError != null,
                supportingText = state.amountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(10.dp)
            )

            // Category with autocomplete
            ExposedDropdownMenuBox(
                expanded         = showCategoryDropdown && state.existingCategories.isNotEmpty(),
                onExpandedChange = { showCategoryDropdown = it }
            ) {
                OutlinedTextField(
                    value         = state.category,
                    onValueChange = { viewModel.onCategoryChanged(it); showCategoryDropdown = true },
                    label         = { Text("Category *") },
                    modifier      = Modifier.fillMaxWidth().menuAnchor(),
                    isError       = state.categoryError != null,
                    supportingText = state.categoryError?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(showCategoryDropdown) },
                    shape         = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded         = showCategoryDropdown &&
                            state.existingCategories.isNotEmpty(),
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    state.existingCategories
                        .filter { it.contains(state.category, ignoreCase = true) }
                        .forEach { suggestion ->
                            DropdownMenuItem(
                                text    = { Text(suggestion) },
                                onClick = {
                                    viewModel.onCategoryChanged(suggestion)
                                    showCategoryDropdown = false
                                }
                            )
                        }
                }
            }

            // Date picker
            OutlinedTextField(
                value = state.expenseDate.format(
                    DateTimeFormatter.ofPattern("MMM d, yyyy")),
                onValueChange = {},
                label = { Text("Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                enabled = false,
                trailingIcon = {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Notes
            OutlinedTextField(
                value         = state.notes,
                onValueChange = viewModel::onNotesChanged,
                label         = { Text("Notes (optional)") },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 3,
                shape         = RoundedCornerShape(10.dp)
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = { viewModel.save(businessId, staffId) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = !state.isLoading,
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (state.isEditMode) "Update expense" else "Save expense",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // ── Date picker dialog ────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.expenseDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.onDateChanged(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}