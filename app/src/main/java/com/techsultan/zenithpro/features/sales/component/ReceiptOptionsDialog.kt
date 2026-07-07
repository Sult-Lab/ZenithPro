package com.techsultan.zenithpro.features.sales.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.techsultan.zenithpro.core.components.ZenithButton

@Composable
fun ReceiptOptionsDialog(
    onPrint: () -> Unit,
    onSharePdf: () -> Unit,
    onShareImage: () -> Unit,
    onDismiss: () -> Unit
) {

    Dialog(
        onDismissRequest = onDismiss
    ) {

        Surface(
            shape = RoundedCornerShape(20.dp)
        ) {

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(60.dp)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Payment Successful",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Choose receipt option"
                )

                Spacer(Modifier.height(24.dp))

                ZenithButton(
                    text = "Print Receipt",
                    onClick = onPrint,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSharePdf
                ) {
                    Text("Share PDF")
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onShareImage
                ) {
                    Text("Share Image")
                }

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Close")
                }
            }
        }
    }
}