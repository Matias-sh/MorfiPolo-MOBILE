package com.cocido.morfipolo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors: ColorScheme = lightColorScheme(
    primary = FoodPrimary,
    onPrimary = FoodTextInverse,
    primaryContainer = FoodPrimarySurface,
    onPrimaryContainer = FoodTextPrimary,
    secondary = FoodSecondary,
    onSecondary = FoodTextInverse,
    background = FoodBackground,
    onBackground = FoodTextPrimary,
    surface = FoodBackgroundCard,
    onSurface = FoodTextPrimary,
    error = FoodError,
    onError = FoodTextInverse,
)

@Composable
fun MorfiPoloTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MorfiTypography,
        shapes = MorfiShapes,
        content = content
    )
}

