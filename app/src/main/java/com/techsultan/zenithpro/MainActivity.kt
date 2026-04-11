package com.techsultan.zenithpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techsultan.zenithpro.core.navigation.RootNavGraph
import com.techsultan.zenithpro.core.theme.ZenithProTheme
import com.techsultan.zenithpro.core.util.AuthState
import com.techsultan.zenithpro.core.viewmodel.DataPersistentViewModel
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataPersistentViewModel: DataPersistentViewModel by inject()

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
}
