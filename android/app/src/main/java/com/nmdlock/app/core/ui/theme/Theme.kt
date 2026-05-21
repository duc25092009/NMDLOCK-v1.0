package com.nmdlock.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// NMDLock brand colors
val Purple600 = Color(0xFF6C5CE7)
val Purple400 = Color(0xFFA29BFE)
val Purple800 = Color(0xFF5A4BD1)
val DarkBg = Color(0xFF0A0A0F)
val DarkSurface = Color(0xFF12121A)
val DarkSurface2 = Color(0xFF1A1A28)
val DarkText = Color(0xFFE0E0E0)
val DarkTextSecondary = Color(0xFF888888)
val LightBg = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF5F5FA)
val LightText = Color(0xFF1A1A2E)
val LightTextSecondary = Color(0xFF666666)
val Success = Color(0xFF2ECC71)
val Warning = Color(0xFFF39C12)
val Error = Color(0xFFE74C3C)
val Info = Color(0xFF3498DB)

private val DarkColorScheme = darkColorScheme(
    primary = Purple600,
    onPrimary = Color.White,
    primaryContainer = Purple800,
    onPrimaryContainer = Purple400,
    secondary = Purple400,
    onSecondary = Color.Black,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = DarkTextSecondary,
    error = Error,
    onError = Color.White,
    outline = DarkSurface2,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple600,
    onPrimary = Color.White,
    primaryContainer = Purple400,
    onPrimaryContainer = Color.Black,
    secondary = Purple600,
    onSecondary = Color.White,
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightTextSecondary,
    error = Error,
    onError = Color.White,
    outline = LightSurface,
)

/**
 * NMDLock Material 3 theme with dark/light support.
 */
@Composable
fun NMDLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
