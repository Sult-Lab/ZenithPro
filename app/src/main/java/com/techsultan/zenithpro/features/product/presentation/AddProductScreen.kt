package com.techsultan.zenithpro.features.product.presentation

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.techsultan.zenithpro.core.components.CustomTextField
import com.techsultan.zenithpro.core.components.DatePickerDialog
import com.techsultan.zenithpro.core.components.ZenithButton
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.core.components.checkAndRequestStoragePermission
import com.techsultan.zenithpro.core.components.rememberStoragePermissionLauncher
import com.techsultan.zenithpro.features.product.data.remote.AddProductRequest
import com.techsultan.zenithpro.features.product.data.remote.ProductVariantCreateRequest
import com.techsultan.zenithpro.features.product.data.remote.StockCreateRequest
import com.techsultan.zenithpro.features.product.data.remote.VariantAttributeInput
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navigateBack: () -> Unit = {},
    onScanBarcode: () -> Unit = {},
    viewModel: AddProductViewModel
) {
    val context = LocalContext.current
    var productName by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var salesPrice by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var stockQuantity by remember { mutableStateOf("") }
    var lowStockAlert by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var warningDay by remember { mutableStateOf("") }
    var expandWarningDay by remember { mutableStateOf(false) }
    var trackExpiryDate by remember { mutableStateOf(false) }
    var datePickerDialog by remember { mutableStateOf(false) }
    var productImageUris by rememberSaveable { mutableStateOf(listOf<Uri>()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Variations state
    var showVariationsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var variations by remember {
        mutableStateOf(
            listOf(
                VariationType("Size", Icons.Default.Straighten, emptyList()),
                VariationType("Color", Icons.Default.Palette, emptyList())
            )
        )
    }

    val state by viewModel.state
    val scannedBarcode by viewModel.scannedBarcode

    LaunchedEffect(scannedBarcode) {
        scannedBarcode?.let {
            barcode = it
            viewModel.clearScannedBarcode()
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddProductViewModel.UiEvent.Success -> {
                    Toast.makeText(context, "Product added successfully", Toast.LENGTH_SHORT).show()
                    // Clear product state
                    productName = ""
                    productDescription = ""
                    category = ""
                    salesPrice = ""
                    costPrice = ""
                    stockQuantity = ""
                    lowStockAlert = ""
                    expiryDate = ""
                    barcode = ""
                    trackExpiryDate = false
                    productImageUris = emptyList()
                    variations = listOf(
                        VariationType("Size", Icons.Default.Straighten, emptyList()),
                        VariationType("Color", Icons.Default.Palette, emptyList())
                    )
                }
                is AddProductViewModel.UiEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

     val imagePicker = rememberLauncherForActivityResult(
         contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
     ) { uris ->
         productImageUris = uris
         selectedImageUri = uris.firstOrNull()
    }

    val storagePermissionLauncher = rememberStoragePermissionLauncher(
        onPermissionGranted = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    )


    Scaffold(
        topBar = {
            ZenithTopAppBar(
                title = "Add New Product",
                navigationIcon = {
                    IconButton(onClick = { navigateBack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Add Product Image
                DottedBorderBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    if (productImageUris.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {

                            selectedImageUri?.let { uri ->
                                Image(
                                    painter = rememberAsyncImagePainter(model = uri),
                                    contentDescription = "product image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            imagePicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                )
                                // Remove image button
                                IconButton(
                                    onClick = {
                                        productImageUris =
                                            productImageUris.filter { it != uri }

                                        selectedImageUri =
                                            productImageUris.firstOrNull()
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove image",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (productImageUris.size > 1){
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    productImageUris.forEach { uri ->
                                        Box {
                                            Image(
                                                painter = rememberAsyncImagePainter(uri),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(70.dp, 80.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(
                                                        width = if (uri == selectedImageUri) 2.dp else 0.dp,
                                                        color = Color.White,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        selectedImageUri = uri
                                                    }
                                            )
                                            IconButton(
                                                onClick = {
                                                    productImageUris =
                                                        productImageUris.filter { it != uri }

                                                    if (selectedImageUri == uri) {
                                                        selectedImageUri =
                                                            productImageUris.firstOrNull()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .background(
                                                        Color.Black.copy(alpha = 0.5f),
                                                        CircleShape
                                                    )
                                                    .size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    checkAndRequestStoragePermission(context, storagePermissionLauncher) {
                                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                }
                        ) {
                            IconButton(
                                onClick = {
                                    checkAndRequestStoragePermission(context, storagePermissionLauncher) {
                                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                },
                                modifier = Modifier.size(50.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color(0xFFE8F5E9),
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add Product Image",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap to upload a photo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Core Details
                SectionHeader("Core Details")
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CustomTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = "Product Name",
                        placeholder = "e.g., Hollandia Yoghurt 1L",
                    )
                    
                    // Category Selector (Simulated with ReadOnly TextField + Icon)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            placeholder = { Text("Select a category") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Pricing & Profit
                SectionHeader("Pricing & Profit")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CustomTextField(
                        value = salesPrice,
                        onValueChange = { salesPrice = it },
                        label = "Sales Price",
                        placeholder = "0.00",
                        prefix = "₦",
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number
                    )
                    CustomTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = "Cost Price",
                        placeholder = "",
                        prefix = "₦",
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number
                    )
                }

                // Stock Management
                SectionHeader("Stock Management")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CustomTextField(
                        value = stockQuantity,
                        onValueChange = { stockQuantity = it },
                        label = "Stock Quantity",
                        placeholder = "0",
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number
                    )
                    CustomTextField(
                        value = lowStockAlert,
                        onValueChange = { lowStockAlert = it },
                        label = "Low Stock Alert",
                        placeholder = "",
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Track Expiry Date",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = trackExpiryDate,
                        onCheckedChange = { trackExpiryDate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2E7D32)
                        )
                    )
                }
                if (trackExpiryDate){

                    CustomTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = "Expiry Date",
                        placeholder = "Select Date",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardType = KeyboardType.Number,
                        trailingIcon = {
                            IconButton(
                                onClick = { datePickerDialog = !datePickerDialog },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Calendar icon"
                                )
                            }
                        }
                    )
                    val warningDays = listOf("30", "40", "50", "60", "70", "80", "90", "100")
                    ExposedDropdownMenuBox(
                        expanded = expandWarningDay,
                        onExpandedChange = { expandWarningDay = it }
                    ) {
                        CustomTextField(
                            value = warningDay,
                            onValueChange = { warningDay = it },
                            label = "Expiry warning",
                            placeholder = "Warn 30 days before",
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                                .fillMaxWidth(),
                            keyboardType = KeyboardType.Number,
                            trailingIcon = {
                                IconButton(
                                    onClick = { expandWarningDay = true },
                                ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Notification"
                                )
                            }
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expandWarningDay,
                        onDismissRequest = { expandWarningDay = false }
                    ) {
                        warningDays.forEach { day ->
                            DropdownMenuItem(
                                text = { Text(text = "Warn $day days before") },
                                onClick = {
                                    warningDay = day
                                    expandWarningDay = false
                                }
                            )
                        }
                    }
                }

                if (datePickerDialog){
                    DatePickerDialog(
                        onDateSelected = { selected ->
                            datePickerDialog = false
                            expiryDate = selected
                        },
                        onDismiss = { datePickerDialog = false },
                    )
                }
            }


            // Advanced Options
            SectionHeader("Advanced Options")
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (barcode.isNotEmpty()) {
                    CustomTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = "Barcode",
                        placeholder = "Product Barcode",
                        trailingIcon = {
                            IconButton(onClick = onScanBarcode) {
                                Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Rescan")
                            }
                        }
                    )
                } else {
                    OutlinedButton(
                        onClick = onScanBarcode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan Barcode",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                OutlinedButton(
                    onClick = { showVariationsSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Style,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (variations.isEmpty()) "Manage Variations" else "Manage Variations (${variations.sumOf { it.items.size }})",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


                ZenithButton(
                    onClick = {
                        val baseSPrice = salesPrice.toLongOrNull() ?: 0L
                        val baseCPrice = costPrice.toLongOrNull() ?: 0L

                        // Mapping variations to ProductVariantCreate
                        val productVariants = mutableListOf<ProductVariantCreateRequest>()
                        variations.forEach { variationType ->
                            variationType.items.forEach { item ->
                                val priceAdj = item.priceAdjustment.toLongOrNull() ?: 0L
                                val itemStock = item.stock.toIntOrNull() ?: 0

                                productVariants.add(
                                    ProductVariantCreateRequest(
                                        sku = "${
                                            productName.take(3).uppercase()
                                        }-${variationType.title.uppercase()}-${item.name.uppercase()}",
                                        salesPrice = baseSPrice + priceAdj,
                                        costPrice = baseCPrice,
                                        barcode = barcode.takeIf { it.isNotEmpty() },
                                        attributes = listOf(
                                            VariantAttributeInput(
                                                optionName = variationType.title,
                                                optionValue = item.name
                                            )
                                        ),
                                        stock = listOf(
                                            StockCreateRequest(
                                                quantity = itemStock,
                                                expiryDate = if (trackExpiryDate) expiryDate else null,
                                                lowStockAlert = lowStockAlert.toIntOrNull()
                                            )
                                        ),
                                        clientId = ""
                                    )
                                )
                            }
                        }

                        viewModel.addProduct(
                            addProductRequest = AddProductRequest(
                                name = productName,
                                description = productDescription,
                                category = category,
                                baseSalesPrice = baseSPrice,
                                baseCostPrice = baseCPrice,
                                expiryWarningDays = warningDay.toIntOrNull(),
                                variants = if (productVariants.isEmpty()) null else productVariants,
                                businessId = viewModel.businessId ?: "",
                                clientId = "",
                                defaultStock = if (productVariants.isEmpty()) {
                                    listOf(
                                        StockCreateRequest(
                                            quantity = stockQuantity.toIntOrNull() ?: 0,
                                            expiryDate = if (trackExpiryDate) expiryDate else null,
                                            lowStockAlert = lowStockAlert.toIntOrNull()
                                        )
                                    )
                                } else emptyList()
                            ),
                            imageUris = productImageUris,
                            barcode = barcode.takeIf { it.isNotEmpty() }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    enabled = !state.isLoading,
                    text = "Save Product"
                )
        }

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00C853))
                }
            }
        }
    }

    if (showVariationsSheet) {
        ManageVariationsBottomSheet(
            sheetState = sheetState,
            variations = variations,
            onDismiss = { showVariationsSheet = false },
            onApply = { updatedVariations ->
                variations = updatedVariations
                showVariationsSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageVariationsBottomSheet(
    sheetState: SheetState,
    variations: List<VariationType>,
    onDismiss: () -> Unit,
    onApply: (List<VariationType>) -> Unit
) {
    var currentVariations by remember { mutableStateOf(variations) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .fillMaxWidth()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Back")
                }
                Text(
                    text = "Manage Variations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Info Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Define variants like Size or Color for this product. Each variant can have its own price adjustment and stock level.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        color = Color(0xFF2E7D32)
                    )
                }

                // Variation Types
                currentVariations.forEachIndexed { index, variationType ->
                    VariationTypeSection(
                        variationType = variationType,
                        onUpdateTitle = { newTitle ->
                            currentVariations = currentVariations.mapIndexed { i, type ->
                                if (i == index) type.copy(title = newTitle) else type
                            }
                        },
                        onRemove = {
                            currentVariations = currentVariations.filterIndexed { i, _ -> i != index }
                        },
                        onAddItem = {
                            currentVariations = currentVariations.mapIndexed { i, type ->
                                if (i == index) {
                                    type.copy(items = type.items + VariationItem())
                                } else type
                            }
                        },
                        onUpdateItem = { itemIndex, updatedItem ->
                            currentVariations = currentVariations.mapIndexed { i, type ->
                                if (i == index) {
                                    val newItems = type.items.toMutableList()
                                    newItems[itemIndex] = updatedItem
                                    type.copy(items = newItems)
                                } else type
                            }
                        }
                    )
                }

                // Add Variation Type Button
                DottedBorderBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable {
                            currentVariations = currentVariations + VariationType("New Variation", Icons.Default.Style, emptyList())
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = Color(0xFF607D8B)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Variation Type (e.g. Material)",
                            color = Color(0xFF607D8B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Footer Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text("Cancel", color = Color.Black)
                }
                Button(
                    onClick = { onApply(currentVariations) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Text("Apply Variations")
                }
            }
        }
    }
}

@Composable
fun VariationTypeSection(
    variationType: VariationType,
    onUpdateTitle: (String) -> Unit,
    onRemove: () -> Unit,
    onAddItem: () -> Unit,
    onUpdateItem: (Int, VariationItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = variationType.icon,
                    contentDescription = null,
                    tint = Color(0xFF00C853)
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                BasicTextField(
                    value = variationType.title,
                    onValueChange = onUpdateTitle,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    cursorBrush = SolidColor(Color.Black),
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Variation: ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            innerTextField()
                        }
                    }
                )
            }
            Text(
                text = "Remove",
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onRemove() }
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                variationType.items.forEachIndexed { index, item ->
                    VariationItemRow(
                        item = item,
                        onUpdate = { onUpdateItem(index, it) }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddItem() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add ${variationType.title} Item",
                        color = Color(0xFF00C853),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun VariationItemRow(
    item: VariationItem,
    onUpdate: (VariationItem) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CustomTextField(
            value = item.name,
            onValueChange = { onUpdate(item.copy(name = it)) },
            label = "NAME",
            placeholder = "e.g. Small",
            modifier = Modifier.weight(1.2f)
        )
        CustomTextField(
            value = item.priceAdjustment,
            onValueChange = { onUpdate(item.copy(priceAdjustment = it)) },
            label = "PRICE ADJ.",
            placeholder = "0",
            prefix = "₦",
            modifier = Modifier.weight(1f),
            keyboardType = KeyboardType.Number
        )
        CustomTextField(
            value = item.stock,
            onValueChange = { onUpdate(item.copy(stock = it)) },
            label = "STOCK",
            placeholder = "0",
            modifier = Modifier.weight(0.8f),
            keyboardType = KeyboardType.Number
        )
    }
}

@Composable
fun DottedBorderBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val stroke = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    val color = MaterialTheme.colorScheme.outlineVariant
    
    Box(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = color,
                    style = stroke,
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .background(Color(0xFFF5F7FA), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

data class VariationItem(
    val name: String = "",
    val priceAdjustment: String = "",
    val stock: String = ""
)

data class VariationType(
    val title: String,
    val icon: ImageVector,
    val items: List<VariationItem> = emptyList()
)
