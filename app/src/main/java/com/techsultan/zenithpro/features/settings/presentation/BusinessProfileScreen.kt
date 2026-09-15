package com.techsultan.zenithpro.features.settings.presentation

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.techsultan.zenithpro.R
import com.techsultan.zenithpro.core.components.CustomTextField
import com.techsultan.zenithpro.core.components.ZenithButton
import com.techsultan.zenithpro.core.components.ZenithLogoSection
import com.techsultan.zenithpro.core.components.ZenithPhoneNumberField
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.core.components.checkAndRequestStoragePermission
import com.techsultan.zenithpro.core.components.rememberStoragePermissionLauncher
import com.techsultan.zenithpro.core.util.BusinessConstants
import org.koin.androidx.compose.koinViewModel

@Composable
fun BusinessProfileScreen(
    viewModel: EditBusinessViewModel = koinViewModel(),
    onBack: () -> Unit = {},
    onSaveChanges: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var showTypePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditBusinessViewModel.EditBusinessEvent.Saved -> {
                    snackbarHost.showSnackbar("Business profile updated")
                    onSaveChanges()
                }
                is EditBusinessViewModel.EditBusinessEvent.ShowError ->
                    snackbarHost.showSnackbar(event.message)
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.onLogoSelected(it) }
    }

    val storagePermissionLauncher = rememberStoragePermissionLauncher(
        onPermissionGranted = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHost) },
            topBar = {
                ZenithTopAppBar(
                    title = "",
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                ZenithButton(
                    text = stringResource(id = R.string.save_changes),
                    onClick = viewModel::save,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    enabled = !state.isLoading
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
                item {
                    ZenithLogoSection(
                        logoUri = state.logoDisplayUri,
                        onPick = {
                            checkAndRequestStoragePermission(context, storagePermissionLauncher) {
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        },
                        onRemove = viewModel::onRemoveLogo
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionHeader(title = stringResource(id = R.string.general_details))

                        CustomTextField(
                            value = state.businessName,
                            onValueChange = viewModel::onNameChanged,
                            label = stringResource(id = R.string.business_name),
                            placeholder = stringResource(id = R.string.placeholder_business_name),
                            error = state.nameError
                        )

                        BusinessTypeField(
                            value = state.businessType.ifBlank { "Select type" },
                            expanded = showTypePicker,
                            onExpandedChange = { showTypePicker = it },
                            onTypeSelected = { type ->
                                viewModel.onTypeChanged(type)
                                showTypePicker = false
                            },
                            currentType = state.businessType
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionHeader(title = stringResource(id = R.string.contact_location))

                        ZenithPhoneNumberField(
                            label = stringResource(id = R.string.phone_number),
                            value = state.phoneNumber,
                            onValueChange = viewModel::onPhoneChanged,
                            placeholder = stringResource(id = R.string.placeholder_phone_number)
                        )

                        CustomTextField(
                            value = state.emailAddress,
                            onValueChange = viewModel::onEmailChanged,
                            label = stringResource(id = R.string.email_address),
                            placeholder = stringResource(id = R.string.placeholder_email),
                            keyboardType = KeyboardType.Email
                        )

                        CustomTextField(
                            value = state.physicalAddress,
                            onValueChange = viewModel::onAddressChanged,
                            label = stringResource(id = R.string.physical_address),
                            placeholder = stringResource(id = R.string.placeholder_address),
                            minLines = 3
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionHeader(title = stringResource(id = R.string.localization))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            CustomTextField(
                                value = state.selectedCurrency.name,
                                onValueChange = {},
                                label = stringResource(id = R.string.currency),
                                placeholder = stringResource(id = R.string.placeholder_currency),
                                readOnly = true,
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.Gray.copy(alpha = 0.6f)
                                    )
                                },
                                prefix = state.selectedCurrency.symbol
                            )
                            Text(
                                text = stringResource(id = R.string.currency_locked_msg),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray.copy(alpha = 0.8f),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BusinessTypeField(
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTypeSelected: (String) -> Unit,
    currentType: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(id = R.string.business_type),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
        ExposedDropdownMenuBox(
            expanded  = expanded,
            onExpandedChange = onExpandedChange
        ){
            OutlinedTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                readOnly = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                )
            )
            ExposedDropdownMenu(
                expanded  = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.heightIn(max = 300.dp)
            ){
                BusinessConstants.BUSINESS_TYPES.forEach { type ->
                    DropdownMenuItem(
                        text    = { Text(type) },
                        onClick = { onTypeSelected(type) },
                        trailingIcon = if (currentType == type) {
                            {
                                Icon(Icons.Default.Check, null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BusinessProfileScreenPreview() {
    MaterialTheme {
        BusinessProfileScreen()
    }
}
