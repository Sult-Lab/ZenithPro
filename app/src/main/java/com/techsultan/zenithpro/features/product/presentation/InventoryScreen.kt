package com.techsultan.zenithpro.features.product.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.techsultan.zenithpro.core.components.SyncStatusBadge
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.core.util.Util
import com.techsultan.zenithpro.core.util.Util.formatPrice
import com.techsultan.zenithpro.features.product.component.EmptyInventoryState
import com.techsultan.zenithpro.features.product.component.FilterInventoryBottomSheet
import com.techsultan.zenithpro.features.product.component.SortInventoryBottomSheet
import com.techsultan.zenithpro.features.product.data.local.ProductWithVariants
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onAddProductClick: () -> Unit,
    viewModel: InventoryViewModel = koinViewModel(),
    onProductClick: (String) -> Unit,
    onMenuClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val snackBarHostState = remember { SnackbarHostState() }
    var openMenuDialog by remember { mutableStateOf(false) }
    var sortInventory by remember { mutableStateOf(false) }
    var filterInventory by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is InventoryViewModel.InventoryEvent.ShowError ->
                    snackBarHostState.showSnackbar(event.message)
                is InventoryViewModel.InventoryEvent.ProductDeleted ->
                    snackBarHostState.showSnackbar("Product deleted")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            ZenithTopAppBar(
                title = "Inventory",
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { openMenuDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(color = Color.Transparent)
                    ){
                        DropdownMenu(
                            expanded = openMenuDialog,
                            onDismissRequest = { openMenuDialog = false },
                            modifier = Modifier
                                .background(color = Color.White)
                                .align(Alignment.TopEnd)
                        ){
                            DropdownMenuItem(
                                modifier = Modifier
                                    .width(200.dp),
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            text = "Sort By",
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sort",
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    openMenuDialog = false
                                    sortInventory = true
                                }
                            )
                            DropdownMenuItem(
                                modifier = Modifier
                                    .width(200.dp),
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            text = "Filter",
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FilterAlt,
                                        contentDescription = "Filter",
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    openMenuDialog = false
                                    filterInventory = true
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddProductClick() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Search products...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )

            Box(modifier = Modifier.fillMaxSize()){
                when {
                    state.isLoading && filteredProducts.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    filteredProducts.isEmpty() && !state.isLoading -> {
                        EmptyInventoryState(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = viewModel::refresh,
                        ){
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ){
                                items(
                                    items = filteredProducts,
                                    key = { it.product.id }
                                ){ productWithVariants ->
                                    InventoryItemCard(
                                        productWithVariants = productWithVariants,
                                        onClick = { onProductClick(productWithVariants.product.id) },
                                        onDelete = { viewModel.deleteProduct(productWithVariants.product.id) }
                                    )
                                }
                            }
                        }
                    }
                }
                state.error?.let { error ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) { Text(error) }
                }
            }
        }
    }
    if (sortInventory){
        SortInventoryBottomSheet(
            selectedSort = state.sortOption,
            onDismiss = { sortInventory = false },
            onSortSelected = { viewModel.onSortOptionChanged(it) },
            onReset = { viewModel.resetFilters() }
        )
    }
    if (filterInventory){
        FilterInventoryBottomSheet(
            selectedStockStatus = state.selectedStockStatus,
            minPrice = state.minPrice,
            maxPrice = state.maxPrice,
            onDismiss = { filterInventory = false },
            onApply = { stockStatus, min, max ->
                viewModel.onFilterChanged(stockStatus, min, max)
            },
            onReset = { viewModel.resetFilters() }
        )
    }
}

@Composable
fun InventoryItemCard(
    productWithVariants: ProductWithVariants,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val product = productWithVariants.product
    val totalStock = productWithVariants.variants.sumOf { variant ->
        variant.stock.sumOf { it.quantity }
    }
    val isLowStock = productWithVariants.variants.any { variant ->
        variant.stock.any { s ->
            val threshold = s.lowStockAlert ?: 5
            s.quantity <= threshold
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = product.imageUrls.firstOrNull(),
            contentDescription = product.name,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color = MaterialTheme.colorScheme.surfaceContainer),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically){
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (product.syncStatus != Util.SyncStatus.SYNCED) {
                    SyncStatusBadge(status = product.syncStatus)
                }
            }
            if (product.category != null){
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    totalStock == 0 -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Out of Stock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    isLowStock -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$totalStock in Stock",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFA000)
                        )
                    }
                    else -> {
                        Text(
                            text = "$totalStock in Stock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Text(
            text = "₦${product.baseSalesPrice.formatPrice()}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview
@Composable
fun PreviewInventory() {
    InventoryScreen(
        onAddProductClick = {},
        onProductClick = {}
    )
}
