package com.techsultan.zenithpro.core.components

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.techsultan.zenithpro.core.manager.UpdateState

@Composable
fun UpdateSnackBar(
    updateState: UpdateState,
    onCompleteUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(updateState) {
        if (updateState is UpdateState.Downloaded) {
            snackbarHostState.showSnackbar(
                message = "Update ready to install",
                actionLabel = "Restart"
            ).also {
                if (it == SnackbarResult.ActionPerformed) {
                    onCompleteUpdate()
                }
            }
        } else if (updateState is UpdateState.Failed) {
            snackbarHostState.showSnackbar("Update failed. Try again later.")
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
    )
}