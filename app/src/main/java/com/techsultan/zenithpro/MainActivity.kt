package com.techsultan.zenithpro

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.techsultan.zenithpro.core.navigation.RootNavGraph
import com.techsultan.zenithpro.core.theme.ZenithProTheme
import com.techsultan.zenithpro.core.util.AuthState
import com.techsultan.zenithpro.core.viewmodel.DataPersistentViewModel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.parseSessionFromUrl
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val auth: Auth by inject()
    private var deepLinkUri by mutableStateOf<Uri?>(null)

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

        // Handle deep link from cold start
        handleIntent(intent)

        val dataPersistentViewModel: DataPersistentViewModel by inject()
        requestNotificationPermissionIfNeeded()
        installSplashScreen().setKeepOnScreenCondition {
            dataPersistentViewModel.authState.value == AuthState.Loading
        }
        enableEdgeToEdge()
        setContent {
            ZenithProTheme {
                RootNavGraph(
                    dataPersistentViewModel = dataPersistentViewModel,
                    deepLinkUri = deepLinkUri,
                    onDeepLinkConsumed = { deepLinkUri = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle deep link while app is running
        handleIntent(intent)
    }

    /**
     * Extracts the data URI from the intent and updates the deepLinkUri state.
     * Clears the intent data after extraction to prevent re-processing on configuration changes.
     */
    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                deepLinkUri = uri
                
                // Handle Supabase deep link (recovery tokens or PKCE code)
                val dataString = uri.toString()
                lifecycleScope.launch {
                    try {
                        if (dataString.contains("#access_token=")) {
                            val session = auth.parseSessionFromUrl(dataString)
                            auth.importSession(session)
                        } else if (dataString.contains("code=")) {
                            val code = uri.getQueryParameter("code")
                            if (code != null) {
                                auth.exchangeCodeForSession(code)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "DeepLink Error: ${e.message}")
                    }
                }
                
                // Clear the intent data to avoid re-processing on rotation/recreation
                intent.data = null
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
