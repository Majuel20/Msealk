package com.msealk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MSealkColors = darkColorScheme(
    primary = SealCyan, onPrimary = SealDark,
    secondary = SealPurple, onSecondary = SealDark,
    tertiary = SealPink, background = SealDark,
    onBackground = Color(0xFFF0F5FA), surface = SealSurface,
    onSurface = Color(0xFFF0F5FA), surfaceVariant = Color(0xFF1C2A3B),
    onSurfaceVariant = Color(0xFFAFBED0), outline = Color(0xFF526176),
    error = Color(0xFFFF8892), onError = Color(0xFF370C16)
)

/** Paleta oscura coherente, también para diálogos y controles Material. */
@Composable
fun MSealkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MSealkColors,
        typography = Typography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp)),
        content = content
    )
}
