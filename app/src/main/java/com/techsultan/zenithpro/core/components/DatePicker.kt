package com.techsultan.zenithpro.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.techsultan.zenithpro.core.util.Util.convertLongToFullDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null
) {
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val afterMin = minDateMillis?.let { utcTimeMillis >= it } ?: true
                val beforeMax = maxDateMillis?.let { utcTimeMillis <= it } ?: true
                return afterMin && beforeMax
            }
        }
    )

    val selectedDate = datePickerState.selectedDateMillis?.let {
        convertLongToFullDate(it)
    } ?: ""

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .wrapContentHeight()
                .padding(horizontal = 6.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
        ){
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val scale = if (maxWidth < 360.dp) maxWidth / 360.dp else 1f
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        DatePicker(
                            modifier = Modifier
                                .graphicsLayer(scaleX = scale, scaleY = scale)
                                .width(360.dp * scale),
                            state = datePickerState,
                            showModeToggle = false
                        )
                    }
                }
                Column {
                    HorizontalDivider(
                        thickness = 1.dp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp)
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ZenithButton(
                            text = "Cancel",
                            onClick = { onDismiss() },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ZenithButton(
                            text = "Select",
                            onClick = {
                                onDateSelected(selectedDate)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}