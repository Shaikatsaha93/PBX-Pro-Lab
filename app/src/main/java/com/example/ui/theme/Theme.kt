package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DialerPrimaryDark,
    onPrimary = DialerOnPrimaryDark,
    primaryContainer = DialerPrimaryContainerDark,
    onPrimaryContainer = DialerOnPrimaryContainerDark,
    secondary = DialerSecondaryDark,
    onSecondary = DialerOnSecondaryDark,
    secondaryContainer = DialerSecondaryContainerDark,
    onSecondaryContainer = DialerOnSecondaryContainerDark,
    background = DialerBackgroundDark,
    onBackground = DialerOnBackgroundDark,
    surface = DialerSurfaceDark,
    onSurface = DialerOnSurfaceDark,
    surfaceVariant = DialerSurfaceVariantDark,
    onSurfaceVariant = DialerOnSurfaceVariantDark,
    tertiary = DialerCyanGlow,
    outline = Color(0xFF4A4868),
    outlineVariant = Color(0xFF2E2D48),
    error = CallActionRedDark
)

private val LightColorScheme = lightColorScheme(
    primary = DialerPrimaryLight,
    onPrimary = DialerOnPrimaryLight,
    primaryContainer = DialerPrimaryContainerLight,
    onPrimaryContainer = DialerOnPrimaryContainerLight,
    secondary = DialerSecondaryLight,
    onSecondary = DialerOnSecondaryLight,
    secondaryContainer = DialerSecondaryContainerLight,
    onSecondaryContainer = DialerOnSecondaryContainerLight,
    background = DialerBackgroundLight,
    onBackground = DialerOnBackgroundLight,
    surface = DialerSurfaceLight,
    onSurface = DialerOnSurfaceLight,
    surfaceVariant = DialerSurfaceVariantLight,
    onSurfaceVariant = DialerOnSurfaceVariantLight,
    tertiary = DialerSecondaryLight,
    outline = Color(0xFFB9B7CE),
    outlineVariant = Color(0xFFDCDAEA),
    error = CallActionRed
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SoftphoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalGlassColors provides if (darkTheme) DarkGlassColors else LightGlassColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
