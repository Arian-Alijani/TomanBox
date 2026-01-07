package com.ParsTools.tomanbox.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF0D2B21),
    primaryContainer = MintDark,
    onPrimaryContainer = Color(0xFFB7E4D4),
    secondary = GrayOnSurfaceVariant,
    onSecondary = NightBackground,
    secondaryContainer = NightSurfaceVariant,
    onSecondaryContainer = NightOnSurface,
    tertiary = MintSoft,
    onTertiary = Color(0xFF1B3329),
    background = NightBackground,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = Color(0xFFB4BBC7),
    outline = NightOutline
)

private val LightColorScheme = lightColorScheme(
    primary = Mint,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = MintSoft,
    onPrimaryContainer = MintDeep,
    secondary = GrayOnSurfaceVariant,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = GraySurfaceVariant,
    onSecondaryContainer = Color(0xFF2E333B),
    tertiary = MintDark,
    onTertiary = Color(0xFFFFFFFF),
    background = GrayBackground,
    onBackground = Color(0xFF1F2329),
    surface = GraySurface,
    onSurface = Color(0xFF1F2329),
    surfaceVariant = GraySurfaceVariant,
    onSurfaceVariant = GrayOnSurfaceVariant,
    outline = GrayOutline
)

@Composable
fun TomanBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
