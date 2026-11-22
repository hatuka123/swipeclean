package com.hatuka.income.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFFF7F4EC)
val Card = Color(0xFFFFFFFF)
val CardSoft = Color(0xFFEFEBDF)
val Green = Color(0xFF1F6B4F)
val GreenSoft = Color(0xFFDCEFE4)
val Gold = Color(0xFFD9A62E)
val GoldSoft = Color(0xFFFBF0D2)
val Bad = Color(0xFFC4453A)
val BadSoft = Color(0xFFF8E0DC)
val TextMain = Color(0xFF1E2A24)
val TextDim = Color(0xFF6B7A72)
val Line = Color(0xFFDDD7C8)

/** Colours a workplace can get (no purple). */
val JobColors = listOf(
    Color(0xFF1F6B4F), Color(0xFF2F7FC1), Color(0xFFE07A2E), Color(0xFF1A9AA0),
    Color(0xFFC4453A), Color(0xFF8A5A32), Color(0xFFD9A62E), Color(0xFFD45D8C),
)

fun jobColor(i: Int) = JobColors[((i % JobColors.size) + JobColors.size) % JobColors.size]

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green,
            onPrimary = Color.White,
            primaryContainer = GreenSoft,
            onPrimaryContainer = TextMain,
            secondary = Gold,
            onSecondary = TextMain,
            secondaryContainer = GreenSoft,
            onSecondaryContainer = TextMain,
            background = Bg,
            onBackground = TextMain,
            surface = Card,
            onSurface = TextMain,
            surfaceVariant = CardSoft,
            onSurfaceVariant = TextDim,
            surfaceContainer = Card,
            surfaceContainerHigh = Card,
            surfaceContainerHighest = CardSoft,
            outline = Line,
            error = Bad,
        ),
        content = content,
    )
}
