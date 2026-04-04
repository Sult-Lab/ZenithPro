package com.techsultan.zenithpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.navigation.RootNavGraph
import com.techsultan.zenithpro.core.theme.ZenithProTheme
import com.techsultan.zenithpro.core.viewmodel.DataPersistentViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: DataPersistentViewModel by inject()

        installSplashScreen().setKeepOnScreenCondition {
            viewModel.isLoading.value
        }
        enableEdgeToEdge()
        setContent {
            val isReady by viewModel.isLoading.collectAsStateWithLifecycle()
            if (isReady) return@setContent
            ZenithProTheme {
                RootNavGraph(viewModel = viewModel)
            }
        }
    }
}
