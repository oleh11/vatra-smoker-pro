package com.oleh11.vatrasmoker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Design tokens. Same visual language as ObminFX: soft pastel gradient background with white cards and
 * bold black type in light mode, near-black surfaces with white accents in dark mode.
 * Ember orange is reserved for heat: the gauge, flames and the grill temperature.
 */
@Immutable
data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val backgroundBrush: Brush,
    val card: Color,
    val inset: Color,
    val inset2: Color,
    val line: Color,
    val ink: Color,
    val ink2: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val ember: Color,
    val amber: Color,
    val success: Color,
    val danger: Color
)

val LightAppColors = AppColors(
    isDark = false,
    background = Color(0xFFE4ECF3),
    backgroundBrush = Brush.verticalGradient(
        listOf(Color(0xFFD6EDE6), Color(0xFFDCE6F5), Color(0xFFE8E3F2))
    ),
    card = Color(0xFFFFFFFF),
    inset = Color(0xFFF1F3F6),
    inset2 = Color(0xFFE6E9EE),
    line = Color(0x1A000000),
    ink = Color(0xFF000000),
    ink2 = Color(0xFF26282E),
    muted = Color(0xFF686C76),
    accent = Color(0xFF000000),
    onAccent = Color(0xFFFFFFFF),
    ember = Color(0xFFFF5A1F),
    amber = Color(0xFFF59E0B),
    success = Color(0xFF10B981),
    danger = Color(0xFFEF4444)
)

val DarkAppColors = AppColors(
    isDark = true,
    background = Color(0xFF0E1013),
    backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF0E1013), Color(0xFF14171C))),
    card = Color(0xFF1B1E24),
    inset = Color(0xFF262A32),
    inset2 = Color(0xFF323740),
    line = Color(0x1FFFFFFF),
    ink = Color(0xFFF5F6F8),
    ink2 = Color(0xFFD0D3DA),
    muted = Color(0xFF8E939E),
    accent = Color(0xFFFFFFFF),
    onAccent = Color(0xFF000000),
    ember = Color(0xFFFF5A1F),
    amber = Color(0xFFF59E0B),
    success = Color(0xFF10B981),
    danger = Color(0xFFEF4444)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

// ---- Theme-aware color names used across the screens ----
// (kept from the first design so every screen follows the active theme)

val Accent: Color @Composable get() = LocalAppColors.current.accent
val OnAccent: Color @Composable get() = LocalAppColors.current.onAccent

/** Screen backgrounds are drawn once at the root as a gradient, so surfaces on top stay transparent. */
val Charcoal900: Color @Composable get() = Color.Transparent
val Charcoal800: Color @Composable get() = LocalAppColors.current.card
val Charcoal700: Color @Composable get() = LocalAppColors.current.inset
val Charcoal600: Color @Composable get() = LocalAppColors.current.line
val Charcoal500: Color @Composable get() = LocalAppColors.current.inset2

val TextWhite: Color @Composable get() = LocalAppColors.current.ink
val TextGray: Color @Composable get() = LocalAppColors.current.muted
val TextDim: Color @Composable get() = LocalAppColors.current.muted

val FlameOrange: Color @Composable get() = LocalAppColors.current.ember
val FlameAmber: Color @Composable get() = LocalAppColors.current.amber
val SuccessGreen: Color @Composable get() = LocalAppColors.current.success
val AlertRed: Color @Composable get() = LocalAppColors.current.danger
val WarningYellow: Color @Composable get() = LocalAppColors.current.amber

// Fixed colors (same in both themes)
val SmokerPrimary = Color(0xFFE65100)
val SmokerDark = Color(0xFFBF360C)
val HickoryBrown = Color(0xFF8D6E63)

val Probe1Color = Color(0xFFEF4444) // Red
val Probe2Color = Color(0xFF3B82F6) // Blue
val Probe3Color = Color(0xFF10B981) // Green
val Probe4Color = Color(0xFFF59E0B) // Amber
