package com.allday.detoxy.presentation.ui.timer.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allday.detoxy.domain.model.FocusState
import kotlin.math.cos
import kotlin.math.sin

/**
 * Type B: Zen Timer (명상 모드)
 *
 * 우주/밤하늘 테마의 아름다운 비주얼 타이머입니다.
 * - 깊은 파란색 배경에 별이 반짝이는 효과
 * - 달 모양의 부채꼴로 남은 시간 표시
 * - 호흡하는 듯한 부드러운 애니메이션
 */
@Composable
fun TimerStyleMinimalFlux(
    state: FocusState,
    remainingSeconds: Int,
    totalSeconds: Int,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    // 🔧 남은 시간(분)을 직접 각도로 변환
    // 60분 기준 눈금이므로, 1분 = 6도 (360/60)
    val remainingMinutesFloat = remainingSeconds.toFloat() / 60f
    val runningSweepAngle = remainingMinutesFloat * 6f

    // 포맷된 시간
    val formattedTime = remember(remainingSeconds) {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    // 색상
    val nightBlue = Color(0xFF0D1B2A)
    val deepBlue = Color(0xFF1B263B)
    val starColor = Color(0xFFFFD700)
    val moonColor = Color(0xFFE8E8E8)
    val highlightColor = Color(0xFF4DA8DA)

    // 호흡 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starAlpha"
    )

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // 외부 프레임
        Box(
            modifier = Modifier
                .size(280.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(deepBlue, nightBlue)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(260.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val outerRadius = size.minDimension / 2
                val sectorRadius = outerRadius * 0.85f
                
                // 1. 별 그리기 (랜덤 위치)
                val starPositions = listOf(
                    Offset(center.x - 80, center.y - 60),
                    Offset(center.x + 70, center.y - 80),
                    Offset(center.x + 90, center.y + 30),
                    Offset(center.x - 60, center.y + 70),
                    Offset(center.x + 30, center.y + 90),
                    Offset(center.x - 90, center.y - 10),
                    Offset(center.x + 50, center.y - 40),
                    Offset(center.x - 30, center.y - 90),
                )
                
                starPositions.forEachIndexed { index, pos ->
                    val alpha = if (index % 2 == 0) starAlpha else 1f - starAlpha * 0.5f
                    val starSize = if (index % 3 == 0) 3.dp.toPx() else 2.dp.toPx()
                    drawCircle(
                        color = starColor.copy(alpha = alpha),
                        radius = starSize,
                        center = pos
                    )
                }
                
                // 2. 외곽 눈금 (미세한 점선)
                for (i in 0 until 60) {
                    val angle = i * 6.0 - 90.0
                    val angleRad = Math.toRadians(angle)
                    val isMajor = i % 5 == 0
                    
                    val tickRadius = outerRadius - 6.dp.toPx()
                    val tickSize = if (isMajor) 4.dp.toPx() else 2.dp.toPx()
                    val tickX = center.x + tickRadius * cos(angleRad).toFloat()
                    val tickY = center.y + tickRadius * sin(angleRad).toFloat()
                    
                    drawCircle(
                        color = Color.White.copy(alpha = if (isMajor) 0.6f else 0.25f),
                        radius = tickSize / 2,
                        center = Offset(tickX, tickY)
                    )
                }
                
                // 3. 남은 시간 부채꼴 (달빛 효과)
                // 남은 분을 직접 각도로 변환 (60분 기준 눈금과 일치)
                val sweepAngle = runningSweepAngle
                if (remainingSeconds > 0) {
                    // 반투명 달빛 섹터
                    drawArc(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                highlightColor.copy(alpha = 0.5f),
                                highlightColor.copy(alpha = 0.3f),
                                highlightColor.copy(alpha = 0.1f)
                            ),
                            center = center,
                            radius = sectorRadius
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - sectorRadius, center.y - sectorRadius),
                        size = Size(sectorRadius * 2, sectorRadius * 2)
                    )
                    
                    // 섹터 외곽 글로우
                    drawArc(
                        color = highlightColor.copy(alpha = 0.6f),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - sectorRadius, center.y - sectorRadius),
                        size = Size(sectorRadius * 2, sectorRadius * 2),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                // 4. 중앙 달 (흰색 원)
                val moonRadius = outerRadius * 0.25f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            moonColor,
                            moonColor.copy(alpha = 0.9f)
                        ),
                        center = center,
                        radius = moonRadius
                    ),
                    radius = moonRadius,
                    center = center
                )
            }
            
            // 5. 외곽 숫자 (5분 단위)
            Box(modifier = Modifier.size(260.dp)) {
                val numbers = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
                numbers.forEach { num ->
                    val angle = num * 6.0 - 90.0
                    val angleRad = Math.toRadians(angle)
                    val numberRadius = 105.dp
                    
                    Text(
                        text = num.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.Center)
                            .then(
                                Modifier.offset(
                                    x = (numberRadius.value * cos(angleRad)).dp,
                                    y = (numberRadius.value * sin(angleRad)).dp
                                )
                            )
                    )
                }
            }
            
            // 6. 중앙 시간 텍스트
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * 선형 색상 보간 함수
 */
private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}
