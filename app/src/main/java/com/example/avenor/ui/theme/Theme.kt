package com.example.avenor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AvenorColorScheme = darkColorScheme(
    primary = Accent,
    background = Background,
    surface = Surface,
    onPrimary = Background,
    onBackground = PrimaryText,
    onSurface = PrimaryText
)

@Composable
fun AvenorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AvenorColorScheme,
        content = content
    )
}
