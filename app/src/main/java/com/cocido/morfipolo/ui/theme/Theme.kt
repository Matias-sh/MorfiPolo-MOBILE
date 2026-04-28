package com.cocido.morfipolo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors: ColorScheme = lightColorScheme(
    primary = MorfiOrange,
    onPrimary = MorfiWhite,
    primaryContainer = MorfiOrangeLight,
    onPrimaryContainer = MorfiOrangeDark,
    secondary = MorfiIndigo,
    onSecondary = MorfiWhite,
    secondaryContainer = MorfiIndigoLight,
    onSecondaryContainer = MorfiIndigo,
    background = MorfiBackground,
    onBackground = MorfiGrayDark,
    surface = MorfiWhite,
    onSurface = MorfiGrayDark,
    error = MorfiRed,
    onError = MorfiWhite,
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
