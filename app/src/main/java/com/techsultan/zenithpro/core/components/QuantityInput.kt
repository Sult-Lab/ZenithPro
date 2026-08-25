package com.techsultan.zenithpro.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.techsultan.zenithpro.core.domain.domain.UnitType
import com.techsultan.zenithpro.core.util.Util.formatQuantity

@Composable
fun QuantityInput(
    quantity: Double,
    unitType: UnitType,
    onQuantityChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Quantity",
    enabled: Boolean = true,
    minQuantity: Double = 0.0,
    maxQuantity: Double? = null,
) {
    val step = if (unitType.isDecimal) 0.5 else 1.0

    var text by remember(quantity) {
        mutableStateOf(formatQuantity(quantity, unitType))
    }

    Column(modifier = modifier) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Decrease button
            FilledTonalIconButton(
                onClick = {
                    val newQty = maxOf(minQuantity, quantity - step)
                    onQuantityChange(newQty)
                    text = formatQuantity(newQty, unitType)
                },
                enabled = enabled && quantity > minQuantity,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Decrease",
                    modifier = Modifier.size(16.dp)
                )
            }

            // Text field using CustomTextField
            CustomTextField(
                value = text,
                onValueChange = { input ->
                    // Allow typing freely
                    text = input
                    // Only update quantity when valid
                    val parsed = input.toDoubleOrNull()
                    if (parsed != null && parsed >= minQuantity) {
                        if (maxQuantity == null || parsed <= maxQuantity) {
                            onQuantityChange(parsed)
                        }
                    }
                },
                label = "",
                placeholder = "0",
                keyboardType = if (unitType.isDecimal)
                    KeyboardType.Decimal
                else
                    KeyboardType.Number,
                suffix = unitType.abbreviation,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )

            // Increase button
            FilledTonalIconButton(
                onClick = {
                    val newQty = quantity + step
                    if (maxQuantity == null || newQty <= maxQuantity) {
                        onQuantityChange(newQty)
                        text = formatQuantity(newQty, unitType)
                    }
                },
                enabled = enabled && (maxQuantity == null || quantity < maxQuantity),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Increase",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

