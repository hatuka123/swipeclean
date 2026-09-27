package com.hatuka.swipeclean.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E0FF),
    onPrimaryContainer = Color(0xFF14105E),
    secondary = Color(0xFF5B5D72),
    secondaryContainer = Color(0xFFE0E1F9),
    onSecondaryContainer = Color(0xFF181A2C),
    tertiaryContainer = Color(0xFFFFE8B5),
    onTertiaryContainer = Color(0xFF3A2A00),
    background = Color(0xFFFBFAFF),
    surface = Color(0xFFFBFAFF),
    surfaceContainer = Color(0xFFEFEEF6),
    surfaceContainerHigh = Color(0xFFE9E8F1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBFC2FF),
    onPrimary = Color(0xFF1E1A7A),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E0FF),
    secondary = Color(0xFFC4C5DD),
    secondaryContainer = Color(0xFF434659),
    onSecondaryContainer = Color(0xFFE0E1F9),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFE8B5),
    background = Color(0xFF121218),
    surface = Color(0xFF121218),
    surfaceContainer = Color(0xFF1E1E26),
    surfaceContainerHigh = Color(0xFF292931),
)

/** Colors for the three swipe actions; the same in every theme so the meaning never changes. */
@Immutable
data class ActionColors(val keep: Color, val delete: Color, val move: Color)

private val DefaultActionColors = ActionColors(
    keep = Color(0xFF2EB872),
    delete = Color(0xFFE5484D),
    move = Color(0xFF3B82F6),
)

val LocalActionColors = staticCompositionLocalOf { DefaultActionColors }

@Composable
fun SwipeCleanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalActionColors provides DefaultActionColors) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
