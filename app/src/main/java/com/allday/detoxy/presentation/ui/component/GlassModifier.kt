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
import com.allday.detoxy.presentation.ui.theme.GlassBlack
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
    glassTint: Color = Color.Unspecified
): Modifier {
    val isLight = !isSystemInDarkTheme()
    val borderColor = if (isLight) GlassBorderLight else GlassBorderDark
    
    // Resolve tint color based on theme if not specified
    val resolvedTint = if (glassTint != Color.Unspecified) {
        glassTint
    } else {
        if (isLight) GlassWhite.copy(alpha = alpha) else GlassBlack.copy(alpha = alpha)
    }
    
    // HazeChild applies the blur effect by sampling the "haze source" (background).
    // If hazeState is not provided (e.g. preview mode or fallback), we just use a semi-transparent background.
    val hazeModifier = if (hazeState != null) {
        Modifier.hazeChild(
            state = hazeState,
            style = HazeStyle(
                tint = HazeTint(resolvedTint),
                blurRadius = blurRadius,
            )
        )
    } else {
        // Fallback for when HazeState is missing (or preview)
        // Increase opacity significantly to prevent transparency issues when blur is missing
        val fallbackTint = if (resolvedTint.alpha < 0.9f) {
            resolvedTint.copy(alpha = 0.95f)
        } else {
            resolvedTint
        }
        Modifier.background(fallbackTint, shape)
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

/**
 * 🆕 성능 최적화용 경량 Glass modifier
 * 
 * 실시간 블러 없이 단순 반투명 배경 + 테두리만 적용합니다.
 * LazyColumn/LazyRow 내 리스트 아이템에 사용하여 성능을 개선합니다.
 * 
 * @param shape 컴포넌트 모양
 * @param alpha 배경 투명도
 * @param borderStrokeWidth 테두리 두께
 * @param backgroundColor 커스텀 배경색 (null이면 테마 기본 Glass 색상)
 */
@Composable
fun Modifier.simpleGlass(
    shape: Shape = RoundedCornerShape(16.dp),
    alpha: Float = 0.4f,
    borderStrokeWidth: Dp = 0.5.dp,
    backgroundColor: Color? = null
): Modifier {
    val isLight = !isSystemInDarkTheme()
    val borderColor = if (isLight) GlassBorderLight else GlassBorderDark
    val resolvedBackgroundColor = backgroundColor ?: if (isLight) {
        GlassWhite.copy(alpha = alpha)
    } else {
        GlassBlack.copy(alpha = alpha)
    }
    
    return this
        .shadow(
            elevation = 4.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.05f),
            spotColor = Color.Black.copy(alpha = 0.05f)
        )
        .background(resolvedBackgroundColor, shape)
        .border(
            width = borderStrokeWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.4f),
                    borderColor.copy(alpha = 0.1f)
                )
            ),
            shape = shape
        )
        .clip(shape)
}
