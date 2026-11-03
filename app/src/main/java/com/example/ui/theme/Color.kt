package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Dark Motion Studio Core Palette
val StudioBackground = Color(0xFF090C12)
val StudioSurface = Color(0xFF111622)
val StudioSurfaceVariant = Color(0xFF182032)
val StudioSurfaceElevated = Color(0xFF202A40)
val StudioBorder = Color(0xFF293652)

val StudioTextPrimary = Color(0xFFF3F6FC)
val StudioTextSecondary = Color(0xFF9AA8C7)
val StudioTextMuted = Color(0xFF647494)

// Semantic Status Colors
val StatusConnectedGreen = Color(0xFF10B981)
val StatusWarningAmber = Color(0xFFF59E0B)
val StatusErrorRed = Color(0xFFEF4444)

// Customizable Accent Theme Options
enum class AccentThemeOption(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val container: Color
) {
    CYBER_CYAN(
        id = "cyber_cyan",
        label = "Cyber Cyan",
        primary = Color(0xFF00E5FF),
        secondary = Color(0xFF8B5CF6),
        tertiary = Color(0xFF38BDF8),
        container = Color(0xFF0B2E3B)
    ),
    ELECTRIC_VIOLET(
        id = "electric_violet",
        label = "Electric Violet",
        primary = Color(0xFFA855F7),
        secondary = Color(0xFFEC4899),
        tertiary = Color(0xFF6366F1),
        container = Color(0xFF2B1447)
    ),
    NEON_EMERALD(
        id = "neon_emerald",
        label = "Neon Emerald",
        primary = Color(0xFF10B981),
        secondary = Color(0xFF06B6D4),
        tertiary = Color(0xFF34D399),
        container = Color(0xFF0A2E26)
    ),
    SUNSET_CORAL(
        id = "sunset_coral",
        label = "Sunset Coral",
        primary = Color(0xFFF43F5E),
        secondary = Color(0xFFF59E0B),
        tertiary = Color(0xFFFB7185),
        container = Color(0xFF3B111E)
    ),
    AMBER_GOLD(
        id = "amber_gold",
        label = "Amber Gold",
        primary = Color(0xFFF59E0B),
        secondary = Color(0xFFEF4444),
        tertiary = Color(0xFFFBBF24),
        container = Color(0xFF382408)
    ),
    ROSE_MAGENTA(
        id = "rose_magenta",
        label = "Rose Magenta",
        primary = Color(0xFFEC4899),
        secondary = Color(0xFF00E5FF),
        tertiary = Color(0xFFF472B6),
        container = Color(0xFF3B1028)
    );

    companion object {
        fun fromId(id: String?): AccentThemeOption =
            entries.firstOrNull { it.id == id } ?: CYBER_CYAN
    }
}
