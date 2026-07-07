package com.techsultan.zenithpro.features.inventory.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techsultan.zenithpro.core.components.ZenithButton
import com.techsultan.zenithpro.features.inventory.presentation.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortInventoryBottomSheet(
    selectedSort: SortOption,
    onDismiss: () -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onReset: () -> Unit
){
    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
    ){
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sort By",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Reset",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onReset() }
            )
        }
        RadioButtonSingleSelection(
            modifier = Modifier.fillMaxWidth(),
            selectedOption = selectedSort,
            onOptionSelected = onSortSelected
        )

        ZenithButton(
            text = "Apply Sort",
            onClick = { onDismiss() },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        )
    }
}


@Composable
fun RadioButtonSingleSelection(
    modifier: Modifier = Modifier,
    selectedOption: SortOption,
    onOptionSelected: (SortOption) -> Unit
) {
    val radioOptions = SortOption.entries
    
    Column(modifier.selectableGroup()) {
        radioOptions.forEach { option ->
            Row(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .selectable(
                        selected = (option == selectedOption),
                        onClick = { onOptionSelected(option) },
                        role = Role.RadioButton
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyMedium
                )

                RadioButton(
                    selected = (option == selectedOption),
                    onClick = null
                )
            }
        }
        HorizontalDivider(
            thickness = 1.dp
        )
    }
}
