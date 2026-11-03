package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun MotionLabTheme(
    accentOption: AccentThemeOption = AccentThemeOption.CYBER_CYAN,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = accentOption.primary,
        onPrimary = Color(0xFF06090F),
        primaryContainer = accentOption.container,
        onPrimaryContainer = StudioTextPrimary,
        secondary = accentOption.secondary,
        onSecondary = Color.White,
        secondaryContainer = StudioSurfaceElevated,
        onSecondaryContainer = StudioTextPrimary,
        tertiary = accentOption.tertiary,
        onTertiary = Color(0xFF06090F),
        background = StudioBackground,
        onBackground = StudioTextPrimary,
        surface = StudioSurface,
        onSurface = StudioTextPrimary,
        surfaceVariant = StudioSurfaceVariant,
        onSurfaceVariant = StudioTextSecondary,
        outline = StudioBorder,
        outlineVariant = Color(0xFF1E293B),
        error = StatusErrorRed,
        onError = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StudioShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MotionLabTheme(content = content)
}
