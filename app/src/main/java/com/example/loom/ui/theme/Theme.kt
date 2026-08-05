package com.example.loom.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = SagePrimary,
    secondary = SageSecondary,
    tertiary = AccentWarm,
    background = BackgroundCream,
    surface = SurfaceWhite,
    onPrimary = SurfaceWhite,
    onBackground = TextDark,
    onSurface = TextDark,
    surfaceVariant = BackgroundCream,
    onSurfaceVariant = TextMuted
)

// Notion-style Dark Palette
private val DarkColorScheme = darkColorScheme(
    primary = SagePrimary,
    secondary = SageSecondary,
    tertiary = AccentWarm,
    background = Color(0xFF262626),     // Main screen background (dark grey)
    surface = Color(0xFF181818),        // Bars, cards, and surfaces (darker than main screen)
    surfaceVariant = Color(0xFF1E1E1E), // Secondary surface containers
    onPrimary = Color(0xFF181818),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFE3E3E3),   // Crisp light text
    onSurface = Color(0xFFE3E3E3),
    onSurfaceVariant = Color(0xFFA0A0A0) // Secondary muted text
)

@Composable
fun LoomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Adapts status bar icons: dark icons in light mode, light icons in dark mode
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}