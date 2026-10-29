package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.ThemeMode
import com.example.data.model.ThemePreset

private val CharcoalGreenDarkColorScheme = darkColorScheme(
    primary = EmeraldGreenPrimary,
    onPrimary = Color(0xFF00381B),
    primaryContainer = Color(0xFF00522B),
    onPrimaryContainer = Color(0xFF69F0AE),
    secondary = EmeraldGreenSecondary,
    onSecondary = Color(0xFF00381B),
    background = CharcoalDarkBackground,
    onBackground = CharcoalTextPrimary,
    surface = CharcoalDarkSurface,
    onSurface = CharcoalTextPrimary,
    surfaceVariant = CharcoalDarkSurfaceVariant,
    onSurfaceVariant = CharcoalTextSecondary,
    outline = Color(0xFF334155)
)

private val CharcoalGreenLightColorScheme = lightColorScheme(
    primary = Color(0xFF008947),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F6CA),
    onPrimaryContainer = Color(0xFF00381B),
    secondary = Color(0xFF00796B),
    onSecondary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

private val BlackOrangeDarkColorScheme = darkColorScheme(
    primary = RadiantOrangePrimary,
    onPrimary = Color(0xFF421D00),
    primaryContainer = Color(0xFF632B00),
    onPrimaryContainer = Color(0xFFFFCC80),
    secondary = RadiantOrangeSecondary,
    onSecondary = Color(0xFF421D00),
    background = BlackOrangeBackground,
    onBackground = Color(0xFFF5F5F5),
    surface = BlackOrangeSurface,
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = BlackOrangeSurfaceVariant,
    onSurfaceVariant = Color(0xFFA3A3A3),
    outline = Color(0xFF404040)
)

private val BlackOrangeLightColorScheme = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF4E1D00),
    secondary = Color(0xFFEF6C00),
    onSecondary = Color.White,
    background = Color(0xFFFAF9F6),
    onBackground = Color(0xFF1C1917),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFF5F5F4),
    onSurfaceVariant = Color(0xFF78716C),
    outline = Color(0xFFD6D3D1)
)

private val WhiteGreyColorScheme = lightColorScheme(
    primary = WhiteGreyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCBD5E1),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = WhiteGreySecondary,
    onSecondary = Color.White,
    background = WhiteGreyBackground,
    onBackground = Color(0xFF0F172A),
    surface = WhiteGreySurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = WhiteGreySurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8)
)

private val BlackSilverDarkColorScheme = darkColorScheme(
    primary = SilverPrimary,
    onPrimary = Color(0xFF1A202C),
    primaryContainer = Color(0xFF374151),
    onPrimaryContainer = Color(0xFFF3F4F6),
    secondary = SilverSecondary,
    onSecondary = Color(0xFF1A202C),
    background = BlackSilverBackground,
    onBackground = Color(0xFFF9FAFB),
    surface = BlackSilverSurface,
    onSurface = Color(0xFFF9FAFB),
    surfaceVariant = BlackSilverSurfaceVariant,
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF4B5563)
)

fun getCode0ColorScheme(preset: ThemePreset, isDark: Boolean): ColorScheme {
    return when (preset) {
        ThemePreset.CHARCOAL_GREEN -> if (isDark) CharcoalGreenDarkColorScheme else CharcoalGreenLightColorScheme
        ThemePreset.BLACK_ORANGE -> if (isDark) BlackOrangeDarkColorScheme else BlackOrangeLightColorScheme
        ThemePreset.WHITE_GREY -> if (isDark) CharcoalDarkSurfaceVariant.let {
            darkColorScheme(
                primary = Color(0xFFE2E8F0),
                background = Color(0xFF1E293B),
                surface = Color(0xFF334155),
                surfaceVariant = Color(0xFF475569)
            )
        } else WhiteGreyColorScheme
        ThemePreset.BLACK_SILVER -> if (isDark) BlackSilverDarkColorScheme else lightColorScheme(
            primary = Color(0xFF475569),
            background = Color(0xFFF8FAFC),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE2E8F0)
        )
    }
}

@Composable
fun Code0Theme(
    preset: ThemePreset = ThemePreset.CHARCOAL_GREEN,
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemInDark
    }

    val colorScheme = getCode0ColorScheme(preset, isDark)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
