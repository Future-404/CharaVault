package com.charavault.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlin.math.pow

enum class ThemeMode(val label: String, val icon: String) {
    SYSTEM("跟随系统", ""),
    LIGHT("日间模式", ""),
    DARK("夜间模式", "")
}

private const val MIN_ACCENT_CONTRAST = 3.0
private val DarkBackground = Color(0xFF0F0F14)
private val LightBackground = Color(0xFFF8FAFC)

private fun relativeLuminance(color: Color): Double {
    fun channel(value: Float): Double {
        val normalized = value.toDouble()
        return if (normalized <= 0.03928) {
            normalized / 12.92
        } else {
            ((normalized + 0.055) / 1.055).pow(2.4)
        }
    }

    return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
}

private fun contrastRatio(first: Color, second: Color): Double {
    val firstLuminance = relativeLuminance(first)
    val secondLuminance = relativeLuminance(second)
    val lighter = maxOf(firstLuminance, secondLuminance)
    val darker = minOf(firstLuminance, secondLuminance)
    return (lighter + 0.05) / (darker + 0.05)
}

private fun blendColor(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}

/** Keeps user-selected colors recognizable while making standalone accent content readable. */
internal fun accessibleAccentColor(accentColor: Color, isDark: Boolean): Color {
    val background = if (isDark) DarkBackground else LightBackground
    if (contrastRatio(accentColor, background) >= MIN_ACCENT_CONTRAST) return accentColor

    val contrastTarget = if (isDark) Color.White else Color.Black
    for (step in 1..24) {
        val candidate = blendColor(accentColor, contrastTarget, step / 24f)
        if (contrastRatio(candidate, background) >= MIN_ACCENT_CONTRAST) return candidate
    }
    return contrastTarget
}

internal fun accentContentColor(accentColor: Color): Color {
    val whiteContrast = contrastRatio(Color.White, accentColor)
    val blackContrast = contrastRatio(Color.Black, accentColor)
    return if (whiteContrast >= blackContrast) Color.White else Color.Black
}

@Composable
fun CharaVaultTheme(
    accentColor: Color = PrimaryAccent,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = remember(accentColor, isDark) {
        val accessibleAccent = accessibleAccentColor(accentColor, isDark)
        val accentContent = accentContentColor(accessibleAccent)
        if (isDark) {
            darkColorScheme(
                primary = accessibleAccent,
                secondary = accessibleAccent,
                onPrimary = accentContent,
                onSecondary = accentContent,
                primaryContainer = accessibleAccent.copy(alpha = 0.15f),
                onPrimaryContainer = Color.White,
                background = DarkBackground,
                surface = Color(0xFF191822),
                surfaceVariant = Color(0xFF232230),
                onBackground = Color.White,
                onSurface = Color.White,
                onSurfaceVariant = Color.White.copy(alpha = 0.85f)
            )
        } else {
            lightColorScheme(
                primary = accessibleAccent,
                secondary = accessibleAccent,
                onPrimary = accentContent,
                onSecondary = accentContent,
                primaryContainer = accessibleAccent.copy(alpha = 0.12f),
                onPrimaryContainer = accessibleAccent,
                background = LightBackground,
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFF1F5F9),
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A),
                onSurfaceVariant = Color(0xFF334155)
            )
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
