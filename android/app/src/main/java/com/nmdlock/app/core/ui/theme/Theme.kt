package com.nmdlock.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// NMDLock Cyber Slate Theme — matching NMDLock-App-Preview-v2.html
// Background colors
val DarkBg = Color(0xFF060713)
val DarkBg2 = Color(0xFF0D0E1E)
val DarkSurface = Color(0xFF15182E)
val DarkSurface2 = Color(0xFF1B1E38)

// Text colors
val DarkText = Color(0xFFF1F3F9)
val DarkTextSecondary = Color(0xFF848CA9)

// Accent gradient colors
val Purple600 = Color(0xFF6C5CE7)
val Purple400 = Color(0xFFA29BFE)
val Purple800 = Color(0xFF5A4BD1)
val AccentPurple = Color(0xFF7059FF)
val AccentCyan = Color(0xFF00F2FE)

// Status colors
val Success = Color(0xFF10B981)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)
val Info = Color(0xFF3498DB)

// Border glow
val BorderColor = Color(0x0FFFFFFF) // 6% white
val BorderGlow = Color(0x267059FF)  // 15% purple

// Light theme colors
val LightBg = Color(0xFFF8F9FD)
val LightSurface = Color(0xFFFFFFFF)
val LightText = Color(0xFF1A1A2E)
val LightTextSecondary = Color(0xFF6B7280)

private val DarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = Color.White,
    primaryContainer = Purple800,
    onPrimaryContainer = Purple400,
    secondary = AccentCyan,
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
    outlineVariant = BorderColor,
)

private val LightColorScheme = lightColorScheme(
    primary = AccentPurple,
    onPrimary = Color.White,
    primaryContainer = Purple400,
    onPrimaryContainer = Color.Black,
    secondary = AccentCyan,
    onSecondary = Color.Black,
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
 * NMDLock Material 3 theme — Cyber Slate Edition
 * Matching NMDLock-App-Preview-v2.html design language
 */
@Composable
fun NMDLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
