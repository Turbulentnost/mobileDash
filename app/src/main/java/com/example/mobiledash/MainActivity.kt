package com.example.mobiledash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.mobiledash.ui.MobileDashApp
import com.example.mobiledash.ui.theme.MobileDashTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileDashTheme(darkTheme = false, dynamicColor = false) {
                MobileDashApp(context = this)
            }
        }
    }
}