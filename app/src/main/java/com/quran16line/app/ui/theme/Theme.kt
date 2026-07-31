package com.quran16line.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    secondary = WarmGrey,
    onSecondary = Paper,
    background = Parchment,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Chrome,
    onSurfaceVariant = Muted,
    outline = Rule
)

@Composable
fun QuranTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = AppTypography,
        content = content
    )
}
