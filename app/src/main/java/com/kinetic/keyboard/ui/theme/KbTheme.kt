package com.kinetic.keyboard.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Keyboard color theme (SPEC.md P5.2). Kept as a plain data class (not MaterialTheme) so the IME
 * can swap palettes instantly from a DataStore preference without recomposition of the host.
 */
data class KbTheme(
    val background: Color,
    val key: Color,
    val keyModifier: Color,
    val keyPressed: Color,
    val label: Color,
    val hint: Color,
    val popupBg: Color,
    val accent: Color,
    val stripBg: Color,
    val stripDivider: Color,
) {
    companion object {
        /** The reference photos' look. */
        val Dark = KbTheme(
            background = Color(0xFF141414),
            key = Color(0xFF3B3B3B),
            keyModifier = Color(0xFF262626),
            keyPressed = Color(0xFF5A5A5A),
            label = Color.White,
            hint = Color(0xFFB0B0B0),
            popupBg = Color(0xFF4A4A4A),
            accent = Color(0xFF4FC3F7),
            stripBg = Color(0xFF1E1E1E),
            stripDivider = Color(0xFF3A3A3A),
        )

        val Light = KbTheme(
            background = Color(0xFFECEEF1),
            key = Color.White,
            keyModifier = Color(0xFFCDD3DA),
            keyPressed = Color(0xFFB9C0C8),
            label = Color(0xFF1B1B1D),
            hint = Color(0xFF6E747B),
            popupBg = Color(0xFFDDE1E6),
            accent = Color(0xFF0277BD),
            stripBg = Color(0xFFE2E5E9),
            stripDivider = Color(0xFFC3C9D0),
        )
    }
}

/** User-selectable theme mode. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }
