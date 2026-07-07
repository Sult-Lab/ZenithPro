package com.techsultan.zenithpro.features.category.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techsultan.zenithpro.core.util.Resource
import com.techsultan.zenithpro.features.category.data.local.CategoryEntity
import com.techsultan.zenithpro.features.category.domain.use_case.UpsertCategoryUseCase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    businessId: String,
    upsertCategoryUseCase: UpsertCategoryUseCase,
    onCategorySelected: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var showCreateNew  by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var selectedColor  by remember { mutableStateOf(categoryColors.first()) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filtered = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories
        else categories.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    // Show create option when search has text not matching any category
    val showCreateOption = searchQuery.isNotBlank() &&
            categories.none { it.name.equals(searchQuery.trim(), ignoreCase = true) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Select category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value         = searchQuery,
                onValueChange = {
                    searchQuery   = it
                    showCreateNew = false
                    errorMessage  = null
                },
                placeholder   = { Text("Search or create new...") },
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                trailingIcon  = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = ""; showCreateNew = false }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp)
            )

            // Create new option — appears when search text doesn't match any category
            if (showCreateOption && !showCreateNew) {
                Surface(
                    shape    = RoundedCornerShape(10.dp),
                    color    = Color(0xFF00C853).copy(alpha = 0.08f),
                    border   = BorderStroke(1.dp, Color(0xFF00C853).copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            newCategoryName = searchQuery.trim()
                            showCreateNew   = true
                        }
                ) {
                    Row(
                        modifier          = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.AddCircleOutline,
                            null,
                            tint     = Color(0xFF00C853),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Create \"${searchQuery.trim()}\"",
                            style      = MaterialTheme.typography.bodyMedium,
                            color      = Color(0xFF00C853),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Inline create form
            if (showCreateNew) {
                CreateCategoryInline(
                    initialName    = newCategoryName,
                    selectedColor  = selectedColor,
                    isSaving       = isSaving,
                    errorMessage   = errorMessage,
                    onColorChanged = { selectedColor = it },
                    onNameChanged  = { newCategoryName = it; errorMessage = null },
                    onSave         = {
                        if (newCategoryName.isBlank()) return@CreateCategoryInline
                        isSaving = true
                        scope.launch {
                            val result = upsertCategoryUseCase(
                                id         = null,
                                businessId = businessId,
                                color      = selectedColor,
                                name       = newCategoryName.trim()
                            )
                            isSaving = false
                            when (result) {
                                is Resource.Success -> {
                                    result.data?.let { onCategorySelected(it) }
                                    onDismiss()
                                }
                                is Resource.Error -> {
                                    errorMessage = result.message
                                }
                                else -> Unit
                            }
                        }
                    },
                    onCancel = {
                        showCreateNew = false
                        newCategoryName = ""
                    }
                )
            }

            // Category list
            if (filtered.isEmpty() && searchQuery.isBlank()) {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Category,
                            null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            "No categories yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { showCreateNew = true; newCategoryName = "" }
                        ) {
                            Icon(Icons.Default.Add, null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Create first category")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier            = Modifier.heightIn(max = 320.dp)
                ) {
                    items(filtered, key = { it.id }) { category ->
                        CategoryItem(
                            category   = category,
                            isSelected = category.id == selectedCategoryId,
                            onSelect   = {
                                onCategorySelected(category)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateCategoryInline(
    initialName: String,
    selectedColor: String,
    isSaving: Boolean,
    errorMessage: String?,
    onColorChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier            = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "New category",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value         = initialName,
                onValueChange = onNameChanged,
                label         = { Text("Category name *") },
                modifier      = Modifier.fillMaxWidth(),
                isError       = errorMessage != null,
                supportingText = errorMessage?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                singleLine    = true,
                shape         = RoundedCornerShape(8.dp)
            )

            // Color picker
            Text(
                "Color",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categoryColors) { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                Color(android.graphics.Color.parseColor(color)),
                                CircleShape
                            )
                            .clickable { onColorChanged(color) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColor == color) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint     = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = onCancel,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(8.dp)
                ) { Text("Cancel") }

                Button(
                    onClick  = onSave,
                    enabled  = !isSaving && initialName.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00C853)
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(16.dp),
                            color       = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: CategoryEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val color = remember(category.color) {
        try {
            category.color?.let { Color(android.graphics.Color.parseColor(it)) }
                ?: Color(0xFF757575)
        } catch (e: Exception) {
            Color(0xFF757575)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        shape    = RoundedCornerShape(10.dp),
        color    = if (isSelected) color.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surface,
        border   = if (isSelected) BorderStroke(1.dp, color)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    category.name.first().uppercase(),
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = color
                )
            }
            Text(
                category.name,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                modifier   = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint     = color,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Preset colors for categories
val categoryColors = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7",
    "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
    "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
    "#FFC107", "#FF9800", "#FF5722", "#795548",
    "#607D8B", "#9E9E9E"
)
