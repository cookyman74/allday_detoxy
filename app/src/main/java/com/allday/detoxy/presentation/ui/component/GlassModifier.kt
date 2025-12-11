package com.allday.detoxy.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.allday.detoxy.presentation.ui.theme.GlassBorderDark
import com.allday.detoxy.presentation.ui.theme.GlassBorderLight
import com.allday.detoxy.presentation.ui.theme.GlassWhite
import com.allday.detoxy.presentation.ui.theme.LocalHazeState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import androidx.compose.foundation.isSystemInDarkTheme
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * Applies a "Liquid Glass" (Glassmorphism) effect to the component.
 *
 * @param blurRadius The radius of the blur effect.
 * @param shape The shape of the component (and the blur area).
 * @param alpha The opacity of the glass tint.
 * @param borderStrokeWidth Width of the glass border.
 * @param glassTint The color to tint the glass with (usually white or black with alpha).
 */
@Composable
fun Modifier.liquidGlass(
    hazeState: HazeState? = LocalHazeState.current,
    blurRadius: Dp = 20.dp,
    shape: Shape = RoundedCornerShape(24.dp),
    alpha: Float = 0.4f,
    borderStrokeWidth: Dp = 1.dp,
    glassTint: Color = GlassWhite.copy(alpha = alpha)
): Modifier {
    val isLight = !isSystemInDarkTheme()
    val borderColor = if (isLight) GlassBorderLight else GlassBorderDark
    
    // HazeChild applies the blur effect by sampling the "haze source" (background).
    // If hazeState is not provided (e.g. preview mode or fallback), we just use a semi-transparent background.
    val hazeModifier = if (hazeState != null) {
        Modifier.hazeChild(
            state = hazeState,
            style = HazeStyle(
                tint = HazeTint(glassTint),
                blurRadius = blurRadius,
            )
        )
    } else {
        // Fallback for when HazeState is missing (or preview)
        Modifier.background(glassTint, shape)
    }

    return this
        .shadow(
            elevation = 8.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.1f),
            spotColor = Color.Black.copy(alpha = 0.1f)
        )
        .then(hazeModifier)
        .border(
            width = borderStrokeWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.6f),
                    borderColor.copy(alpha = 0.1f)
                )
            ),
            shape = shape
        )
        .clip(shape)
}

/**
 * Convenience modifier for a lighter glass effect (e.g. for inner items).
 */
@Composable
fun Modifier.liquidGlassLight(
    hazeState: HazeState? = LocalHazeState.current,
    shape: Shape = RoundedCornerShape(16.dp),
): Modifier = liquidGlass(
    hazeState = hazeState,
    blurRadius = 10.dp,
    shape = shape,
    alpha = 0.2f,
    borderStrokeWidth = 0.5.dp
)
