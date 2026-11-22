package com.example.loom.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Every colour role is defined (containers, surface containers, outlines). Roles left unset fall
 * back to Material's default purple, which showed up in chips, segmented buttons and menus.
 */
private fun lightScheme(primary: Color): ColorScheme {
    val onPrimary = if (primary.luminance() > 0.5f) TextDark else SurfaceWhite
    return lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = lerp(primary, SurfaceWhite, 0.78f),
        onPrimaryContainer = lerp(primary, Color.Black, 0.55f),
        secondary = lerp(primary, TextMuted, 0.45f),
        onSecondary = SurfaceWhite,
        secondaryContainer = lerp(primary, SurfaceWhite, 0.85f),
        onSecondaryContainer = TextDark,
        tertiary = AccentWarm,
        onTertiary = TextDark,
        tertiaryContainer = lerp(AccentWarm, SurfaceWhite, 0.6f),
        onTertiaryContainer = TextDark,
        background = BackgroundCream,
        onBackground = TextDark,
        surface = SurfaceWhite,
        onSurface = TextDark,
        surfaceVariant = Color(0xFFEDEBE4),
        onSurfaceVariant = TextMuted,
        surfaceContainerLowest = SurfaceWhite,
        surfaceContainerLow = Color(0xFFF6F5F0),
        surfaceContainer = Color(0xFFF1F0EA),
        surfaceContainerHigh = Color(0xFFECEAE3),
        surfaceContainerHighest = Color(0xFFE6E4DC),
        outline = Color(0xFFB4B9B0),
        outlineVariant = Color(0xFFDCDDD5),
        errorContainer = Color(0xFFFBE3E1),
        onErrorContainer = Color(0xFF7A1C17)
    )
}

private fun darkScheme(primary: Color, amoled: Boolean): ColorScheme {
    // Notion-style dark greys, or true black for OLED screens
    val background = if (amoled) Color.Black else Color(0xFF262626)
    val surface = if (amoled) Color.Black else Color(0xFF181818)
    val container = if (amoled) Color(0xFF0E0E0E) else Color(0xFF1E1E1E)
    return darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF181818),
        primaryContainer = lerp(primary, Color.Black, 0.55f),
        onPrimaryContainer = lerp(primary, Color.White, 0.7f),
        secondary = lerp(primary, Color.White, 0.35f),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = lerp(primary, Color.Black, 0.65f),
        onSecondaryContainer = Color(0xFFE3E3E3),
        tertiary = AccentWarm,
        onTertiary = Color(0xFF181818),
        tertiaryContainer = lerp(AccentWarm, Color.Black, 0.6f),
        onTertiaryContainer = Color(0xFFF3E6DC),
        background = background,
        onBackground = Color(0xFFE3E3E3),
        surface = surface,
        onSurface = Color(0xFFE3E3E3),
        surfaceVariant = container,
        onSurfaceVariant = Color(0xFFA0A0A0),
        surfaceContainerLowest = surface,
        surfaceContainerLow = if (amoled) Color(0xFF080808) else Color(0xFF1B1B1B),
        surfaceContainer = container,
        surfaceContainerHigh = if (amoled) Color(0xFF151515) else Color(0xFF242424),
        surfaceContainerHighest = if (amoled) Color(0xFF1C1C1C) else Color(0xFF2C2C2C),
        outline = Color(0xFF5E625C),
        outlineVariant = Color(0xFF3A3C39),
        errorContainer = Color(0xFF5C1D1A),
        onErrorContainer = Color(0xFFF9DEDC)
    )
}

@Composable
fun LoomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    amoled: Boolean = false,
    accentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val primary = accentColor ?: SagePrimary
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context).let { if (amoled) it.copy(background = Color.Black, surface = Color.Black) else it }
            else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme(primary, amoled)
        else -> lightScheme(primary)
    }

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
