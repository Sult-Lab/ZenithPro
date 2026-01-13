package com.techsultan.zenithpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.techsultan.zenithpro.navigation.RootNavGraph
import com.techsultan.zenithpro.ui.theme.ZenithProTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZenithProTheme {
                RootNavGraph()
            }
        }
    }
}


