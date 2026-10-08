package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AuraDarkColorScheme = darkColorScheme(
    primary = AuraCyan,
    onPrimary = AuraBgDark,
    primaryContainer = AuraSurfaceLightDark,
    onPrimaryContainer = AuraCyan,
    secondary = AuraViolet,
    onSecondary = AuraTextPrimary,
    secondaryContainer = AuraSurfaceLightDark,
    onSecondaryContainer = AuraViolet,
    tertiary = AuraNeonPink,
    background = AuraBgDark,
    onBackground = AuraTextPrimary,
    surface = AuraSurfaceDark,
    onSurface = AuraTextPrimary,
    surfaceVariant = AuraSurfaceLightDark,
    onSurfaceVariant = AuraTextSecondary,
    outline = AuraCardBorder,
    error = AuraError,
    onError = AuraTextPrimary
)

private val AuraLightColorScheme = lightColorScheme(
    primary = AuraCyanDark,
    onPrimary = AuraTextPrimary,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF006064),
    secondary = AuraViolet,
    onSecondary = AuraTextPrimary,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = AuraNeonPink,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = AuraError,
    onError = Color.White
)

@Composable
fun AuraTheme(
    darkTheme: Boolean = true, // Default to futuristic dark mode for Redmi Note 13 AMOLED
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AuraDarkColorScheme else AuraLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun ViernesTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    AuraTheme(darkTheme = darkTheme, content = content)
}
