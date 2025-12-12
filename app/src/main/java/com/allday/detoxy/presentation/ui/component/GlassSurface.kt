package com.allday.detoxy.presentation.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.allday.detoxy.presentation.ui.theme.GlassWhite

/**
 * Glass 스타일 Surface (실시간 블러 적용)
 * 
 * ⚠️ 성능 주의: LazyColumn/LazyRow 내 많은 아이템에 사용 시 성능 저하 발생
 * → 리스트 아이템에는 SimpleGlassSurface 사용 권장
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    blurRadius: Dp = 20.dp,
    alpha: Float = 0.4f,
    tint: Color = GlassWhite.copy(alpha = alpha),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlass(
            shape = shape,
            blurRadius = blurRadius,
            alpha = alpha,
            glassTint = tint
        ),
        content = content
    )
}

/**
 * 🆕 경량 Glass 스타일 Surface (블러 없음)
 * 
 * 실시간 블러 없이 단순 반투명 배경만 적용하여 성능 최적화
 * LazyColumn/LazyRow 내 리스트 아이템에 사용 권장
 * 
 * @param backgroundColor 커스텀 배경색 (null이면 테마 기본 Glass 색상)
 */
@Composable
fun SimpleGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    alpha: Float = 0.4f,
    backgroundColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.simpleGlass(
            shape = shape,
            alpha = alpha,
            backgroundColor = backgroundColor
        ),
        content = content
    )
}
