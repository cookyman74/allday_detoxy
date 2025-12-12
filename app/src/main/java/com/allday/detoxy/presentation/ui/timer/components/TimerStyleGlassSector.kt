package com.allday.detoxy.presentation.ui.timer.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Type C: Visual Timer (타임타이머 스타일)
 *
 * 직관적인 비주얼 타이머를 Glassmorphism으로 재해석한 스타일입니다.
 * - 남은 시간이 빨간색 '부채꼴(Sector)' 모양으로 시각화되어 줄어듭니다.
 * - 외곽 눈금과 숫자로 정확한 시간 인지 가능
 * - 중앙에 디지털 시간 표시
 */
@Composable
fun TimerStyleGlassSector(
    state: FocusState,
    remainingSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier
) {
    // 포맷된 시간 계산
    val formattedTime = remember(remainingSeconds) {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    // 진행률 (남은 시간 비율)
    val progress = remember(remainingSeconds, totalSeconds) {
        if (totalSeconds == 0) 0f
        else remainingSeconds.toFloat() / totalSeconds.toFloat()
    }
    
    // 타임타이머 스타일 색상 (빨간색 계열)
    val sectorColor = Color(0xFFE53935) // 빨간색
    val sectorColorLight = Color(0xFFFF5252)
    val backgroundColor = Color.White.copy(alpha = 0.95f)
    val tickColor = Color(0xFF424242)
    val numberColor = Color(0xFF616161)

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // 외부 프레임 (그림자 효과)
        Box(
            modifier = Modifier
                .size(280.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(260.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val outerRadius = size.minDimension / 2
                val innerRadius = outerRadius * 0.35f // 중앙 흰색 원 반경
                val sectorRadius = outerRadius * 0.85f // 부채꼴 반경
                
                // 1. 외곽 눈금 그리기 (0-60)
                for (i in 0 until 60) {
                    val angle = i * 6.0 - 90.0 // 12시 방향 기준
                    val angleRad = Math.toRadians(angle)
                    
                    val isMajor = i % 5 == 0
                    val tickOuterRadius = outerRadius - 4.dp.toPx()
                    val tickLength = if (isMajor) 14.dp.toPx() else 6.dp.toPx()
                    val tickWidth = if (isMajor) 2.5.dp.toPx() else 1.5.dp.toPx()
                    
                    val startX = center.x + (tickOuterRadius - tickLength) * cos(angleRad).toFloat()
                    val startY = center.y + (tickOuterRadius - tickLength) * sin(angleRad).toFloat()
                    val endX = center.x + tickOuterRadius * cos(angleRad).toFloat()
                    val endY = center.y + tickOuterRadius * sin(angleRad).toFloat()

                    drawLine(
                        color = tickColor.copy(alpha = if (isMajor) 0.8f else 0.4f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = tickWidth,
                        cap = StrokeCap.Round
                    )
                }
                
                // 2. 남은 시간 부채꼴 그리기 (빨간색 섹터)
                val sweepAngle = 360f * progress
                
                if (progress > 0f) {
                    // 부채꼴 영역 (빨간색)
                    drawArc(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                sectorColorLight,
                                sectorColor,
                                sectorColor.copy(alpha = 0.9f)
                            ),
                            center = center,
                            radius = sectorRadius
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle, // 시계 방향으로 채움
                        useCenter = true,
                        topLeft = Offset(center.x - sectorRadius, center.y - sectorRadius),
                        size = Size(sectorRadius * 2, sectorRadius * 2)
                    )
                    
                    // 부채꼴 테두리
                    drawArc(
                        color = sectorColor.copy(alpha = 0.3f),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - sectorRadius, center.y - sectorRadius),
                        size = Size(sectorRadius * 2, sectorRadius * 2),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                
                // 3. 중앙 흰색 원 (디지털 시계 배경)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFF5F5F5)
                        ),
                        center = center,
                        radius = innerRadius
                    ),
                    radius = innerRadius,
                    center = center
                )
                
                // 중앙 원 테두리
                drawCircle(
                    color = Color(0xFFE0E0E0),
                    radius = innerRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            
            // 4. 외곽 숫자 표시 (0, 5, 10, ..., 55)
            Box(modifier = Modifier.size(260.dp)) {
                val numbers = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
                numbers.forEach { num ->
                    val angle = num * 6.0 - 90.0
                    val angleRad = Math.toRadians(angle)
                    val numberRadius = 100.dp
                    
                    Text(
                        text = num.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = numberColor,
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
            
            // 5. 중앙 시간 텍스트
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121),
                    letterSpacing = 2.sp
                )
                Text(
                    text = "M          S",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF9E9E9E),
                    letterSpacing = 8.sp
                )
            }
        }
    }
}
