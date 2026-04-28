package com.cocido.morfipolo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val MorfiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(24.dp), 
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp)
)

object AppRadius {
    val small = 8.dp
    val medium = 16.dp
    val card = 32.dp // More rounded as per redesign
    val button = 100.dp // Pill shape
    val input = 12.dp
    val extraLarge = 40.dp
}
