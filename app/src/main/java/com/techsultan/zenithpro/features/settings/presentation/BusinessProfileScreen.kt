package com.techsultan.zenithpro.features.settings.presentation

import android.net.Uri
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
                    LogoSection(
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

                        PhoneNumberField(
                            value = state.phoneNumber,
                            onValueChange = viewModel::onPhoneChanged
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
private fun LogoSection(
    modifier: Modifier = Modifier,
    logoUri: Any?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            if (logoUri != null) {
                AsyncImage(
                    model = logoUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentScale = ContentScale.Crop,
                )
                IconButton(
                    onClick  = onRemove,
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            CircleShape
                        )
                        .offset(x = 4.dp, y = 4.dp)
                ){
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clickable{ onPick() }
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F171B)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    )
                }
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = R.string.your_store_logo),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(id = R.string.logo_subtext),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Button(
            onClick = onPick,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = stringResource(id = R.string.change_logo),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
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

@Composable
private fun PhoneNumberField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(id = R.string.phone_number),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Surface(
                modifier = Modifier
                    .width(80.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                color = Color(0xFFF9FAFB),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+234",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(id = R.string.placeholder_phone_number)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                ),
                singleLine = true
            )
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
