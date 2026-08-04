package com.example.loom.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
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

@Composable
fun LoomTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // We removed the deprecated statusBarColor here!
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}