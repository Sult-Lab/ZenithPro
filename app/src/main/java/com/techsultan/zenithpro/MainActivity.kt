package com.techsultan.zenithpro

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.techsultan.zenithpro.core.navigation.RootNavGraph
import com.techsultan.zenithpro.core.theme.ZenithProTheme
import com.techsultan.zenithpro.core.util.AuthState
import com.techsultan.zenithpro.core.viewmodel.DataPersistentViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            lifecycleScope.launch {

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataPersistentViewModel: DataPersistentViewModel by inject()
        requestNotificationPermissionIfNeeded()
        installSplashScreen().setKeepOnScreenCondition {
            dataPersistentViewModel.authState.value == AuthState.Loading
        }
        enableEdgeToEdge()
        setContent {
            ZenithProTheme {
                RootNavGraph(dataPersistentViewModel = dataPersistentViewModel)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted) {
                lifecycleScope.launch {
                }
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            lifecycleScope.launch {
            }
        }
    }
}
