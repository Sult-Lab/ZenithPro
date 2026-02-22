package com.techsultan.zenithpro.core.components

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun rememberStoragePermissionLauncher(
    onPermissionGranted: () -> Unit,
): ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>> {
    val context = LocalContext.current
    return rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasReadPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions[READ_MEDIA_IMAGES] == true ||
                    permissions[READ_MEDIA_VISUAL_USER_SELECTED] == true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[READ_MEDIA_IMAGES] == true
        } else {
            permissions[READ_EXTERNAL_STORAGE] == true
        }

        if (hasReadPermission) {
            onPermissionGranted()
        } else {
            Toast.makeText(context, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}

fun checkAndRequestStoragePermission(
    context: Context,
    launcher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>,
    onPermissionGranted: () -> Unit
) {
    when {
        // For Android 14 (API 34) and above
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            val hasFullAccess = ContextCompat.checkSelfPermission(
                context,
                READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
            if (hasFullAccess) {
                onPermissionGranted()
            } else {
                launcher.launch(
                    arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED)
                )
            }
        }
        // For Android 13 (API 33)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                onPermissionGranted()
            } else {
                launcher.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO))
            }
        }
        // For Android 12 (API 32) and below
        else -> {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                onPermissionGranted()
            } else {
                launcher.launch(arrayOf(READ_EXTERNAL_STORAGE))
            }
        }
    }
}