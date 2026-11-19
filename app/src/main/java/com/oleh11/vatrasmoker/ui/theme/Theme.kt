package com.oleh11.vatrasmoker.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** How the app picks light or dark: follow the phone (default) or force one. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

private fun schemeFor(c: AppColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.accent, onPrimary = c.onAccent,
        primaryContainer = c.inset, onPrimaryContainer = c.ink,
        secondary = c.ember, onSecondary = c.onAccent,
        background = c.background, onBackground = c.ink,
        surface = c.card, onSurface = c.ink,
        surfaceVariant = c.inset, onSurfaceVariant = c.muted,
        outline = c.line, outlineVariant = c.line,
        error = c.danger, onError = c.onAccent
    )
} else {
    lightColorScheme(
        primary = c.accent, onPrimary = c.onAccent,
        primaryContainer = c.inset, onPrimaryContainer = c.ink,
        secondary = c.ember, onSecondary = c.onAccent,
        background = c.background, onBackground = c.ink,
        surface = c.card, onSurface = c.ink,
        surfaceVariant = c.inset, onSurfaceVariant = c.muted,
        outline = c.line, outlineVariant = c.line,
        error = c.danger, onError = c.onAccent
    )
}

@Composable
fun CabelasSmokerCompanionTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkAppColors else LightAppColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = schemeFor(colors),
            typography = Typography
        ) {
            // One gradient behind every screen, like ObminFX
            Box(modifier = Modifier.fillMaxSize().background(colors.backgroundBrush)) {
                content()
            }
        }
    }
}
