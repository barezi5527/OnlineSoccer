package com.onlinesoccer.app.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E3B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9EF0B5),
    onPrimaryContainer = Color(0xFF00210D),
    secondary = Color(0xFF4C6354),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCEE8D4),
    onSecondaryContainer = Color(0xFF081F13),
    background = Color(0xFFF8FAF8),
    onBackground = Color(0xFF191C19),
    surface = Color(0xFFF8FAF8),
    onSurface = Color(0xFF191C19),
    surfaceVariant = Color(0xFFDDE5DE),
    onSurfaceVariant = Color(0xFF414942),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82D49D),
    onPrimary = Color(0xFF00391B),
    primaryContainer = Color(0xFF005227),
    onPrimaryContainer = Color(0xFF9EF0B5),
    secondary = Color(0xFFB2CCB9),
    onSecondary = Color(0xFF1E3526),
    secondaryContainer = Color(0xFF354B3C),
    onSecondaryContainer = Color(0xFFCEE8D4),
    background = Color(0xFF11140F),
    onBackground = Color(0xFFE0E4DE),
    surface = Color(0xFF11140F),
    onSurface = Color(0xFFE0E4DE),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFC1C9C1),
    error = Color(0xFFFFB4AB),
)

@Composable
fun OnlineSoccerTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}