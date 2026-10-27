package com.myraa.assistant.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class NovaColorScheme(
    val background: Color,
    val backgroundGradientTop: Color,
    val surface: Color,
    val surfaceGlass: Color,
    val surfaceBorder: Color,
    val surfaceRaised: Color,
    val accent: Color,
    val accentSecondary: Color,
    val accentGlow: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val userBubble: Color,
    val assistantBubble: Color,
    val depthShadow: Color,
    val rimLight: Color,
    val lagoon: Color,
    val lotus: Color
)

val DarkNovaColors = NovaColorScheme(
    background = Color(0xFF06050E),
    backgroundGradientTop = Color(0xFF15113A),
    surface = Color(0xFF17143A),
    surfaceGlass = Color(0xB81A1644),
    surfaceBorder = Color(0x406A60B0),
    surfaceRaised = Color(0xFF221E4D),
    accent = Color(0xFFFFB547),
    accentSecondary = Color(0xFFFF8A3D),
    accentGlow = Color(0x59FFB547),
    onAccent = Color(0xFF15112E),
    textPrimary = Color(0xFFEEE9FF),
    textSecondary = Color(0xFFA7A0CC),
    textTertiary = Color(0xFF7A73A6),
    success = Color(0xFF3FE0D0),
    warning = Color(0xFFFFB547),
    error = Color(0xFFFF5C6C),
    userBubble = Color(0xFF282156),
    assistantBubble = Color(0xFF161338),
    depthShadow = Color(0x99000000),
    rimLight = Color(0x2EFFFFFF),
    lagoon = Color(0xFF3FE0D0),
    lotus = Color(0xFFFF6FAE)
)

val LightNovaColors = NovaColorScheme(
    background = Color(0xFFF3F0FB),
    backgroundGradientTop = Color(0xFFE5E0F6),
    surface = Color(0xFFFFFFFF),
    surfaceGlass = Color(0xCCFFFFFF),
    surfaceBorder = Color(0x2615112E),
    surfaceRaised = Color(0xFFEAE5F8),
    accent = Color(0xFFB86A00),
    accentSecondary = Color(0xFFD97706),
    accentGlow = Color(0x33B86A00),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF15112E),
    textSecondary = Color(0xFF4C456E),
    textTertiary = Color(0xFF7B739E),
    success = Color(0xFF0D9488),
    warning = Color(0xFFD97706),
    error = Color(0xFFDC2626),
    userBubble = Color(0xFFE2DCF7),
    assistantBubble = Color(0xFFFFFFFF),
    depthShadow = Color(0x1F000000),
    rimLight = Color(0x40FFFFFF),
    lagoon = Color(0xFF0D9488),
    lotus = Color(0xFFDB2777)
)

val LocalNovaColors = staticCompositionLocalOf { DarkNovaColors }
val LocalNovaReducedMotion = staticCompositionLocalOf { false }
