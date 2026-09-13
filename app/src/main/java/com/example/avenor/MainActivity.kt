package com.example.avenor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.avenor.ui.AvenorApp
import com.example.avenor.ui.theme.AvenorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AvenorTheme {
                AvenorApp()
            }
        }
    }
}
