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
 * Type A: Liquid Ring (Signature Glass)
 * 
 * 디톡시의 시그니처인 '액체(Liquid)'와 '유리(Glass)' 질감을 활용한 타이머 스타일입니다.
 * 깔끔한 원형 진행바와 눈금으로 시간을 직관적으로 표시합니다.
 *
 * @param state 타이머 상태
 * @param remainingSeconds 남은 시간 (초)
 * @param totalSeconds 전체 시간 (초)
 * @param modifier Modifier
 */
@Composable
fun TimerStyleLiquidRing(
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

    // 진행률 계산 (남은 시간 비율)
    val progress = remember(remainingSeconds, totalSeconds) {
        if (totalSeconds == 0) 0f
        else remainingSeconds.toFloat() / totalSeconds.toFloat()
    }

    // 색상
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = Color.White.copy(alpha = 0.95f)
    val trackColor = Color(0xFFE0E0E0)
    val tickColor = Color(0xFF9E9E9E)

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
                val ringRadius = outerRadius * 0.78f
                val ringWidth = 14.dp.toPx()
                
                // 1. 외곽 눈금 그리기
                for (i in 0 until 60) {
                    val angle = i * 6.0 - 90.0
                    val angleRad = Math.toRadians(angle)
                    
                    val isMajor = i % 5 == 0
                    val tickOuterRadius = outerRadius - 8.dp.toPx()
                    val tickLength = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                    val tickWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                    
                    val startX = center.x + (tickOuterRadius - tickLength) * cos(angleRad).toFloat()
                    val startY = center.y + (tickOuterRadius - tickLength) * sin(angleRad).toFloat()
                    val endX = center.x + tickOuterRadius * cos(angleRad).toFloat()
                    val endY = center.y + tickOuterRadius * sin(angleRad).toFloat()

                    drawLine(
                        color = tickColor.copy(alpha = if (isMajor) 0.7f else 0.3f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = tickWidth,
                        cap = StrokeCap.Round
                    )
                }
                
                // 2. 배경 링 (트랙)
                drawCircle(
                    color = trackColor,
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = ringWidth, cap = StrokeCap.Round)
                )
                
                // 3. 진행률 링 (Primary Color)
                val sweepAngle = 360f * progress
                if (progress > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.8f),
                                primaryColor,
                                primaryColor.copy(alpha = 0.9f)
                            ),
                            center = center
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                        size = Size(ringRadius * 2, ringRadius * 2),
                        style = Stroke(width = ringWidth, cap = StrokeCap.Round)
                    )
                    
                    // 진행 끝점 강조 (원형 캡)
                    val endAngleRad = Math.toRadians(-90.0 + sweepAngle)
                    val endX = center.x + ringRadius * cos(endAngleRad).toFloat()
                    val endY = center.y + ringRadius * sin(endAngleRad).toFloat()
                    
                    drawCircle(
                        color = primaryColor,
                        radius = ringWidth / 2 + 2.dp.toPx(),
                        center = Offset(endX, endY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(endX, endY)
                    )
                }
            }
            
            // 4. 외곽 숫자 표시
            Box(modifier = Modifier.size(260.dp)) {
                val numbers = listOf(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60)
                val displayNumbers = listOf("5", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55", "0")
                
                numbers.forEachIndexed { index, num ->
                    val angle = num * 6.0 - 90.0
                    val angleRad = Math.toRadians(angle)
                    val numberRadius = 105.dp
                    
                    Text(
                        text = displayNumbers[index],
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF757575),
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
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121),
                    letterSpacing = 2.sp
                )
                Text(
                    text = "분",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}
