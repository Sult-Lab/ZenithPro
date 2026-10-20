package com.techsultan.zenithpro.features.material.presentation

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.techsultan.zenithpro.core.components.ZenithTopAppBar
import com.techsultan.zenithpro.features.sales.formatAmount
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun RecipeListRoute(
    viewModel: RecipeListViewModel = koinViewModel(),
    onNewRecipe: () -> Unit,
    onEditRecipe: (variantId: String) -> Unit,
    onStartBatch: (variantId: String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    RecipeListScreen(
        state = state,
        onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
        onCategorySelect = { viewModel.onCategorySelected(it) },
        onNewRecipeClick = onNewRecipe,
        onEditRecipe = onEditRecipe,
        onStartBatch = onStartBatch,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    state: RecipeListUiState,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onNewRecipeClick: () -> Unit,
    onEditRecipe: (variantId: String) -> Unit,
    onStartBatch: (variantId: String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            ZenithTopAppBar(
                title = "Product Recipes (BOM)",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {}
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewRecipeClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Recipe", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary Stats Section
            SummaryStatsHeader(stats = state.summaryStats)

            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search recipes by product name or code...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            // Category Filter Chips
            if (state.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedCategory == null,
                            onClick = { onCategorySelect(null) },
                            label = {
                                Text(
                                    text = "All (${state.allRecipes.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (state.selectedCategory == null) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }

                    items(state.categories) { cat ->
                        val count = state.allRecipes.count { it.category.equals(cat, ignoreCase = true) }
                        val isSelected = state.selectedCategory.equals(cat, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategorySelect(if (isSelected) null else cat) },
                            label = {
                                Text(
                                    text = "$cat ($count)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // Recipe Items List or Empty State
            if (state.filteredRecipes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Text(
                            text = if (state.searchQuery.isNotBlank()) "No recipes match '${state.searchQuery}'" else "No Product Recipes Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Create a recipe (Bill of Materials) for your products to calculate unit cost, margins, and manage material stock automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = onNewRecipeClick,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Build First Recipe")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.filteredRecipes, key = { it.variantId }) { recipeItem ->
                        RecipeCardItem(
                            recipe = recipeItem,
                            onEditRecipe = { onEditRecipe(recipeItem.variantId) },
                            onStartBatch = { onStartBatch(recipeItem.variantId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatsHeader(stats: RecipeSummaryStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Total Recipes
        StatBox(
            modifier = Modifier.weight(1f),
            title = "Total Recipes",
            value = "${stats.totalRecipes} SKUs",
            subtitle = "Catalog",
            icon = Icons.Default.Inventory2,
            iconTint = MaterialTheme.colorScheme.primary
        )

        // Avg Margin
        StatBox(
            modifier = Modifier.weight(1f),
            title = "Avg Margin",
            value = String.format(Locale.US, "%.1f%%", stats.avgMarginPercent),
            subtitle = "Target >50%",
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            iconTint = Color(0xFF2E7D32)
        )

        // Ready to Produce
        StatBox(
            modifier = Modifier.weight(1f),
            title = "Ready to Bake",
            value = "${stats.readyToProduceCount} / ${stats.totalRecipes}",
            subtitle = if (stats.totalRecipes > 0) "${(stats.readyToProduceCount * 100 / stats.totalRecipes)}% stocked" else "0% stocked",
            icon = Icons.Default.CheckCircle,
            iconTint = Color(0xFF2E7D32)
        )
    }
}

@Composable
private fun StatBox(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun RecipeCardItem(
    recipe: RecipeItemUiState,
    onEditRecipe: () -> Unit,
    onStartBatch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Thumbnail + Title/SKU
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box {
                    if (!recipe.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = recipe.imageUrl,
                            contentDescription = recipe.productName,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    if (!recipe.category.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(bottomEnd = 8.dp, topStart = 12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Text(
                                text = recipe.category,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "SKU: ${recipe.variantSku}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onEditRecipe,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Metric Breakdown Box: Unit Cost | Selling Price | Margin
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricColumn(
                        label = "Unit Cost",
                        value = "₦${String.format(Locale.US, "%,.2f", recipe.unitCost)}"
                    )

                    MetricColumn(
                        label = "Selling Price",
                        value = "₦${recipe.sellingPrice.formatAmount()}"
                    )

                    // Margin % Badge
                    val (badgeBg, badgeText) = when {
                        recipe.marginPercent >= 40.0 -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                        recipe.marginPercent >= 20.0 -> Color(0xFFFFF3E0) to Color(0xFFE65100)
                        else -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Margin",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f%%", recipe.marginPercent),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Ingredients Summary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${recipe.ingredientsCount} Ingredients • ${recipe.ingredientsSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Real-time Stock Status Badge
            when (val status = recipe.stockStatus) {
                is RecipeStockStatus.InStock -> {
                    StatusBadge(
                        containerColor = Color(0xFFE8F5E9),
                        contentColor = Color(0xFF1B5E20),
                        icon = Icons.Default.CheckCircle,
                        text = "All Materials In Stock • Can produce ~${status.maxProducibleUnits} units"
                    )
                }
                is RecipeStockStatus.LowStock -> {
                    StatusBadge(
                        containerColor = Color(0xFFFFF8E1),
                        contentColor = Color(0xFFF57F17),
                        icon = Icons.Default.WarningAmber,
                        text = "Low Raw Materials • ${status.lowMaterialName} Low (Max ${status.maxProducibleUnits} units)"
                    )
                }
                is RecipeStockStatus.Blocked -> {
                    StatusBadge(
                        containerColor = Color(0xFFFFEBEE),
                        contentColor = Color(0xFFC62828),
                        icon = Icons.Default.Block,
                        text = "Stockout on ${status.blockedMaterialName} • Production Blocked"
                    )
                }
            }

            // Bottom Actions: View BOM | Start Batch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEditRecipe,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View BOM")
                }

                Button(
                    onClick = onStartBatch,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (recipe.stockStatus is RecipeStockStatus.Blocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (recipe.stockStatus is RecipeStockStatus.Blocked) Icons.Default.WarningAmber else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (recipe.stockStatus is RecipeStockStatus.Blocked) "Reorder" else "Start Batch")
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusBadge(
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
