package com.allday.detoxy.presentation.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Mesh Gradient Mockup (Using combined radial gradients or linear gradients)
// In a real production environment, you might use an image or a ShaderBrush for better performance and visual.

val MeshGradientBackgroundLight = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFE0C3FC), // Light Purple
        Color(0xFF8EC5FC), // Light Blue
        Color(0xFFFFFFFF)  // White
    )
)

val MeshGradientBackgroundDark = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF2E1C4A), // Dark Purple
        Color(0xFF1A1F3D), // Dark Blue
        Color(0xFF000000)  // Black
    )
)

// More complex mock mesh gradient
fun getMeshGradient(isDark: Boolean): Brush {
    return if (isDark) {
        Brush.linearGradient(
            0.0f to Color(0xFF2E3192),
            1.0f to Color(0xFF1BFFFF),
            start = Offset(0f, 0f),
            end = Offset(1000f, 1000f)
        )
    } else {
        Brush.linearGradient(
            0.0f to Color(0xFFD4FC79),
            1.0f to Color(0xFF96E6A1),
            start = Offset(0f, 0f),
            end = Offset(1000f, 1000f)
        )
    }
}
