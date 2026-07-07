@file:OptIn(ExperimentalMaterial3Api::class)

package com.techsultan.zenithpro.features.inventory.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.techsultan.zenithpro.core.components.CustomTextField
import com.techsultan.zenithpro.core.components.ZenithButton

@Composable
fun FilterInventoryBottomSheet(
    selectedStockStatus: String,
    minPrice: Long?,
    maxPrice: Long?,
    onDismiss: () -> Unit,
    onApply: (String, Long?, Long?) -> Unit,
    onReset: () -> Unit
) {
    var localStockStatus by remember { mutableStateOf(selectedStockStatus) }
    var minText by remember { mutableStateOf(minPrice?.toString() ?: "") }
    var maxText by remember { mutableStateOf(maxPrice?.toString() ?: "") }

    // Simplified slider logic for now as it needs a range
    var sliderPosition by remember { mutableStateOf(0f..1000000f) }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
    ){
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onDismiss() }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "close"
                )
            }
            Text(
                text = "Filter Inventory",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Reset",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { 
                    onReset()
                    onDismiss()
                }
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            thickness = 0.5.dp
        )

        Text(
            text = "Stock Status",
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
        
        StockStatusSelection(
            modifier = Modifier.fillMaxWidth(),
            selectedOption = localStockStatus,
            onOptionSelected = { localStockStatus = it }
        )

        Text(
            text = "Price Range",
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ){
            CustomTextField(
                value = minText,
                onValueChange = { minText = it },
                label = "MIN",
                placeholder = "0",
                prefix = "₦",
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
            CustomTextField(
                value = maxText,
                onValueChange = { maxText = it },
                label = "MAX",
                placeholder = "500000",
                prefix = "₦",
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZenithButton(
                modifier = Modifier
                    .weight(0.4f),
                text = "Clear All",
                onClick = {
                    onReset()
                    onDismiss()
                }
            )
            ZenithButton(
                modifier = Modifier
                    .weight(0.6f),
                text = "Show Results",
                onClick = {
                    onApply(
                        localStockStatus,
                        minText.toLongOrNull(),
                        maxText.toLongOrNull()
                    )
                    onDismiss()
                }
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
    }
}

@Composable
fun StockStatusSelection(
    modifier: Modifier = Modifier,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
){
    val radioOptions = listOf(
        FilterRadioOption( text = "All items", color = Color(0xFF1976D2)),
        FilterRadioOption( text = "In Stock", color =Color(0xFF18512B)),
        FilterRadioOption( text = "Low Stock", color =Color(0xFFFFA000)),
        FilterRadioOption( text = "Out of Stock", color =Color(0xFFD32F2F)),
    )
    
    Column(modifier.selectableGroup()){
        radioOptions.forEach { item ->
            val isSelected = item.text == selectedOption
            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            val surfaceColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface

            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onOptionSelected(item.text) },
                border = BorderStroke(1.dp, color = borderColor),
                color = surfaceColor,
                shape = RoundedCornerShape(12.dp)
            ){
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .selectable(
                            selected = isSelected,
                            onClick = { onOptionSelected(item.text) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(item.color)
                    )
                }
            }
        }
    }
}

data class FilterRadioOption(
    val text: String,
    val color: Color
)
