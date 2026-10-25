package com.example.avenor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Avenor design tokens — identical hex values to iOS Theme.swift and the website's tokens.css
 * (see DESIGN_SYSTEM.md). Aesthetic-Usability Effect: one calm, consistent system on every platform.
 */
object Av {
    val Background = Color(0xFF08090B)
    val Surface = Color(0xFF111318)
    val Elevated = Color(0xFF181B21)
    val Border = Color(0xFF292D35)
    val Text = Color(0xFFF5F5F5)
    val Muted = Color(0xFF969BA6)
    val Accent = Color(0xFF8B9CFF)
    val AccentDeep = Color(0xFF5C54F0)
    val Green = Color(0xFF65D6A0)
    val Red = Color(0xFFFF7373)
    val Orange = Color(0xFFFFB86B)
    val Pink = Color(0xFFFA8CBE)
    val Sky = Color(0xFF73CDFA)
    val Violet = Color(0xFFC8A0FF)
    val Gold = Color(0xFFFFD666)

    val ClassPalette = listOf(Accent, Green, Orange, Pink, Sky, Violet, Gold, Red)
    fun classColor(index: Int): Color = ClassPalette[((index % ClassPalette.size) + ClassPalette.size) % ClassPalette.size]

    /** Von Restorff: only the single most important block on a screen uses this. */
    val HeroGradient = Brush.linearGradient(listOf(AccentDeep, Accent))

    // Fitts's Law
    val MinTap = 48.dp          // Android's minimum touch target (iOS uses 44 pt)
    val PrimaryHeight = 56.dp

    // Radii
    val RadiusS = 14.dp
    val RadiusM = 16.dp
    val RadiusL = 20.dp
    val RadiusXL = 24.dp
}

private val scheme = darkColorScheme(
    primary = Av.Accent,
    onPrimary = Color.Black,
    secondary = Av.Green,
    background = Av.Background,
    onBackground = Av.Text,
    surface = Av.Surface,
    onSurface = Av.Text,
    surfaceVariant = Av.Elevated,
    onSurfaceVariant = Av.Muted,
    surfaceContainer = Av.Surface,
    surfaceContainerHigh = Av.Elevated,
    surfaceContainerHighest = Av.Elevated,
    outline = Av.Border,
    outlineVariant = Av.Border,
    error = Av.Red,
)

private val type = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun AvenorTheme(content: @Composable () -> Unit) {
    // Avenor is dark-only on every platform, like the iOS app.
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
