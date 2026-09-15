package com.techsultan.zenithpro.features.inventory.presentation

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.techsultan.zenithpro.core.components.CustomTextField
import com.techsultan.zenithpro.core.components.QuantityInput
import com.techsultan.zenithpro.core.components.ZenithButton
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.core.components.checkAndRequestStoragePermission
import com.techsultan.zenithpro.core.components.rememberStoragePermissionLauncher
import com.techsultan.zenithpro.core.domain.domain.UnitType
import com.techsultan.zenithpro.core.util.Util.formatPrice
import com.techsultan.zenithpro.core.util.Util.formatRelativeTime
import com.techsultan.zenithpro.features.category.presentation.CategoryPickerSheet
import com.techsultan.zenithpro.features.inventory.data.local.ProductAuditLogEntity
import com.techsultan.zenithpro.features.inventory.data.local.ProductEntity
import com.techsultan.zenithpro.features.inventory.data.remote.ProductVariantCreateRequest
import com.techsultan.zenithpro.features.inventory.data.remote.StockCreateRequest
import com.techsultan.zenithpro.features.inventory.data.remote.UpdateProductRequest
import com.techsultan.zenithpro.features.inventory.data.remote.VariantAttributeInput
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBack: () -> Unit,
    onScanBarcode: () -> Unit,
    viewModel: ProductDetailViewModel
) {
    val context = LocalContext.current
    val state by viewModel.state
    var isEditing by remember { mutableStateOf(false) }


    var productName by remember(productId) { mutableStateOf("") }
    var category by remember(productId) { mutableStateOf("") }
    var selectedCategoryId by remember(productId) { mutableStateOf<String?>(null) }
    var salesPrice by remember(productId) { mutableStateOf("") }
    var costPrice by remember(productId) { mutableStateOf("") }
    var barcode by remember(productId) { mutableStateOf("") }
    var stockQuantity by remember(productId) { mutableStateOf(0.0) }
    var lowStockAlert by remember(productId) { mutableStateOf("") }
    var productImageUris by remember(productId) { mutableStateOf(listOf<Uri>()) }
    var selectedImageUri by remember(productId) { mutableStateOf<Uri?>(null) }
    val selectedUnitType = UnitType.fromString(state.unitType)
    var expandUnitType by remember { mutableStateOf(false) }
    var variations by remember(productId) {
        mutableStateOf(
            listOf(
                VariationType("Size", Icons.Default.Straighten, emptyList()),
                VariationType("Color", Icons.Default.Palette, emptyList())
            )
        )
    }
    var showVariationsSheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

    LaunchedEffect(productId) {
        viewModel.getProduct(productId)
        viewModel.loadAuditLogs(productId)
    }

    LaunchedEffect(state.product) {
        state.product?.let { pWithV ->
            val p = pWithV.product
            productName = p.name
            category = p.category ?: ""
            salesPrice = p.baseSalesPrice.toString()
            costPrice = p.baseCostPrice.toString()
            barcode = pWithV.variants.firstOrNull()?.variant?.barcode ?: ""
            productImageUris = p.imageUrls.map { it.toUri() }
            
            if (selectedImageUri == null) {
                selectedImageUri = productImageUris.firstOrNull()
            }

            // Map variants to VariationType
            val groupedByAttribute = pWithV.variants.flatMap { v ->
                v.attributes.map { it.optionName to (it.optionValue to v) }
            }.groupBy({ it.first }, { it.second })

            if (groupedByAttribute.isNotEmpty()) {
                variations = groupedByAttribute.map { (name, values) ->
                    VariationType(
                        title = name,
                        icon = when (name.lowercase()) {
                            "size" -> Icons.Default.Straighten
                            "color" -> Icons.Default.Palette
                            else -> Icons.Default.Style
                        },
                        items = values.map { (value, v) ->
                            VariationItem(
                                name = value,
                                priceAdjustment = (v.variant.salesPrice - p.baseSalesPrice).toString(),
                                stock = com.techsultan.zenithpro.core.util.Util.formatQuantity(
                                    v.stock.sumOf { it.quantity },
                                    UnitType.fromString(p.unitType)
                                )
                            )
                        }
                    )
                }
            }
            
            stockQuantity = pWithV.variants.sumOf { v -> v.stock.sumOf { it.quantity } }
            lowStockAlert = pWithV.variants.firstOrNull()?.stock?.firstOrNull()?.lowStockAlert?.toString() ?: ""
        }
    }

    LaunchedEffect(viewModel.scannedBarcode.value) {
        viewModel.scannedBarcode.value?.let {
            barcode = it
            viewModel.clearScannedBarcode()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ProductDetailViewModel.UiEvent.Success -> {
                    Toast.makeText(context, "Product updated", Toast.LENGTH_SHORT).show()
                    isEditing = false
                }
                is ProductDetailViewModel.UiEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            productImageUris = uris
            selectedImageUri = uris.firstOrNull()
        }
    }

    val storagePermissionLauncher = rememberStoragePermissionLauncher(
        onPermissionGranted = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    )

    Scaffold(
        topBar = {
            ZenithTopAppBar(
                title = if (isEditing) "Edit Product" else "Product Details",
                navigationIcon = {
                    IconButton(onClick = { if (isEditing) isEditing = false else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (state.isLoading && state.product == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.error != null && state.product == null) {
                Text(text = state.error!!, modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (isEditing) {
                        // Edit UI
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
                                                        PickVisualMediaRequest(
                                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                                        )
                                                    )
                                                }
                                        )
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
                                                .background(
                                                    Color.Black.copy(alpha = 0.5f),
                                                    CircleShape
                                                )
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
                                    if (productImageUris.size > 1) {
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
                        SectionHeader("Core Details")
                        CustomTextField(
                            value = productName,
                            onValueChange = { productName = it },
                            label = "Product Name",
                            placeholder = "Enter product name"
                        )

                        // Category Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Category",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = category,
                                onValueChange = { },
                                placeholder = { Text("Select a category") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCategorySheet = true },
                                readOnly = true,
                                enabled = false,
                                trailingIcon = {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Sold by",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val selectedUnitType = UnitType.fromString(state.unitType)

                            ExposedDropdownMenuBox(
                                expanded = expandUnitType,
                                onExpandedChange = { expandUnitType = it }
                            ) {
                                OutlinedTextField(
                                    value = "${selectedUnitType.label} (${selectedUnitType.abbreviation})",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandUnitType)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = expandUnitType,
                                    onDismissRequest = { expandUnitType = false }
                                ) {
                                    // Group by category
                                    val groups = mapOf(
                                        "Discrete" to listOf(
                                            UnitType.UNIT,
                                            UnitType.PIECE,
                                            UnitType.PACK,
                                            UnitType.DOZEN,
                                            UnitType.CARTON,
                                            UnitType.BAG,
                                            UnitType.SACHET,
                                            UnitType.BOTTLE,
                                            UnitType.TIN,
                                            UnitType.ROLL,
                                            UnitType.BUNDLE,
                                            UnitType.PALLET
                                        ),
                                        "Weight" to listOf(UnitType.KILOGRAM, UnitType.GRAM, UnitType.POUND),
                                        "Volume" to listOf(UnitType.LITRE, UnitType.MILLILITRE, UnitType.GALLON),
                                        "Length" to listOf(UnitType.METRE, UnitType.YARD, UnitType.FOOT),
                                    )

                                    groups.forEach { (groupName, units) ->
                                        // Group header
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    groupName.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            onClick = {},
                                            enabled = false
                                        )
                                        units.forEach { unit ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(unit.label)
                                                        Text(
                                                            unit.abbreviation,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.onUnitTypeChanged(unit.name)
                                                    expandUnitType = false
                                                },
                                                leadingIcon = if (unit == selectedUnitType) {
                                                    {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            null,
                                                            tint = Color(0xFF00C853)
                                                        )
                                                    }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }
                        }

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
                                placeholder = "0.00",
                                prefix = "₦",
                                modifier = Modifier.weight(1f),
                                keyboardType = KeyboardType.Number
                            )
                        }

                        SectionHeader("Stock Management")
                        QuantityInput(
                            quantity = stockQuantity,
                            unitType = selectedUnitType,
                            onQuantityChange = { stockQuantity = it },
                            label = "Stock Quantity",
                            minQuantity = 0.0,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        CustomTextField(
                            value = lowStockAlert,
                            onValueChange = { lowStockAlert = it },
                            label = "Low Stock Alert",
                            placeholder = "",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardType = KeyboardType.Number
                        )

                        if (barcode.isNotEmpty()) {
                            CustomTextField(
                                value = barcode,
                                onValueChange = { barcode = it },
                                label = "Barcode",
                                placeholder = "Barcode",
                                trailingIcon = {
                                    IconButton(onClick = onScanBarcode) {
                                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Rescan")
                                    }
                                }
                            )
                        } else {
                            OutlinedButton(
                                onClick = onScanBarcode,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Outlined.QrCodeScanner, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Scan Barcode")
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
                                text = if (variations.all { it.items.isEmpty() }) "Manage Variations" else "Manage Variations (${variations.sumOf { it.items.size }})",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        ZenithButton(
                            onClick = {
                                val baseSPrice = salesPrice.toLongOrNull() ?: 0L
                                val baseCPrice = costPrice.toLongOrNull() ?: 0L

                                // Mapping variations to ProductVariantCreate
                                val productVariants = mutableListOf<ProductVariantCreateRequest>()
                                variations.forEach { variationType ->
                                    variationType.items.forEach { item ->
                                        val priceAdj = item.priceAdjustment.toLongOrNull() ?: 0L
                                        val itemStock = item.stock.toDoubleOrNull() ?: 0.0

                                        productVariants.add(
                                            ProductVariantCreateRequest(
                                                sku = state.product?.variants?.firstOrNull()?.variant?.sku ?: "${
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
                                                        expiryDate = state.product?.variants?.firstOrNull()?.stock?.firstOrNull()?.expiryDate,
                                                        lowStockAlert = lowStockAlert.toIntOrNull()
                                                    )
                                                ),
                                                clientId = "" 
                                            )
                                        )
                                    }
                                }

                                viewModel.updateProduct(
                                    updateProductRequest = UpdateProductRequest(
                                        clientId = productId,
                                        name = productName,
                                        description = state.product?.product?.description,
                                        category = category,
                                        baseSalesPrice = baseSPrice,
                                        baseCostPrice = baseCPrice,
                                        expiryWarningDays = state.product?.product?.expiryWarningDays,
                                        businessId = viewModel.businessId ?: "",
                                        variants = productVariants,
                                        defaultStock = if (productVariants.isEmpty()) {
                                            listOf(
                                                StockCreateRequest(
                                                    quantity = stockQuantity,
                                                    expiryDate = state.product?.variants?.firstOrNull()?.stock?.firstOrNull()?.expiryDate,
                                                    lowStockAlert = lowStockAlert.toIntOrNull()
                                                )
                                            )
                                        } else emptyList()
                                    ),
                                    imageUris = productImageUris,
                                    barcode = barcode
                                )
                            },
                            text = "Save Changes",
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading
                        )

                    } else {
                        // View UI
                        state.product?.let { pWithV ->
                            val product = pWithV.product

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(product.imageUrls) { imageUrl ->
                                    DottedBorderBox(
                                        modifier = Modifier
                                            .fillParentMaxWidth()
                                            .height(200.dp)
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(imageUrl),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.background)
                                                .fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }

                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )

                            product.category?.let {
                                Text(text = it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Sales Price", style = MaterialTheme.typography.labelMedium)
                                    Text("₦${product.baseSalesPrice.formatPrice()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Cost Price", style = MaterialTheme.typography.labelMedium)
                                    Text("₦${product.baseCostPrice.formatPrice()}", style = MaterialTheme.typography.titleLarge)
                                }
                            }

                            val totalStock = pWithV.variants.sumOf { v -> v.stock.sumOf { it.quantity } }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Current Stock", fontWeight = FontWeight.Bold)
                                    Text("$totalStock units", style = MaterialTheme.typography.titleMedium)
                                }
                            }

                            if (pWithV.variants.size > 1 || pWithV.variants.firstOrNull()?.attributes?.isNotEmpty() == true) {
                                Text("Variants", fontWeight = FontWeight.Bold)
                                pWithV.variants.forEach { vWithS ->
                                    val attrText = vWithS.attributes.joinToString(", ") { "${it.optionName}: ${it.optionValue}" }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(attrText.ifEmpty { "Default" })
                                        Text(
                                        com.techsultan.zenithpro.core.util.Util.formatStockDisplay(
                                            vWithS.stock.sumOf { it.quantity },
                                            UnitType.fromString(product.unitType)
                                        )
                                    )
                                    }
                                }
                            }
                            ProductAuditSection(
                                product = product,
                                auditLogs = auditLogs,
                                canViewAudit = viewModel.canViewAudit
                            )
                        }
                    }
                }
            }
            if (state.isLoading && state.product != null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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

    if (showCategorySheet) {
        CategoryPickerSheet(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            businessId = viewModel.businessId ?: "",
            upsertCategoryUseCase = viewModel.upsertCategoryUseCase,
            onCategorySelected = {
                category = it.name
                selectedCategoryId = it.id
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false }
        )
    }
}

@Composable
fun ProductAuditSection(
    product: ProductEntity,
    auditLogs: List<ProductAuditLogEntity>,
    canViewAudit: Boolean,  // isManager or isAdmin
) {
    if (!canViewAudit) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Last updated by
        if (product.updatedByName != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = "Last updated by",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = product.updatedByName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = formatRelativeTime(product.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Price change history — admin only
        if (auditLogs.isNotEmpty()) {
            HorizontalDivider()

            Text(
                text = "Change history",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            auditLogs.take(5).forEach { log ->
                AuditLogItem(log = log)
            }
        }
    }
}

@Composable
fun AuditLogItem(log: ProductAuditLogEntity) {
    val description = when (log.changeType) {
        "PRODUCT_CREATED" -> "Product created"
        "PRODUCT_EDITED"  -> "Product details updated"
        "PRICE_CHANGE"    -> buildString {
            val field = if (log.fieldChanged == "sales_price") "Selling price" else "Cost price"
            val oldFormatted = log.oldValue?.toLongOrNull()?.let { "₦${it / 100}" } ?: log.oldValue
            val newFormatted = log.newValue?.toLongOrNull()?.let { "₦${it / 100}" } ?: log.newValue
            append("$field changed")
            if (log.variantSku != null) append(" (${log.variantSku})")
            append(": $oldFormatted → $newFormatted")
        }
        "VARIANT_ADDED"   -> "Variant added: ${log.variantSku}"
        "VARIANT_REMOVED" -> "Variant removed: ${log.variantSku}"
        "STOCK_ADJUSTMENT"-> "Stock adjusted"
        else -> log.changeType
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "by ${log.changedByName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatRelativeTime(log.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
