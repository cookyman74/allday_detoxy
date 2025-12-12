package com.allday.detoxy.presentation.ui.timer.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allday.detoxy.domain.model.FocusState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Type C: Visual Timer (타임타이머 스타일 - Interactive)
 *
 * 직관적인 비주얼 타이머 - 빨간색 부채꼴로 시간을 표시합니다.
 * IDLE 상태에서 드래그로 시간 설정 가능
 */
@Composable
fun TimerStyleGlassSector(
    selectedMinutes: Int = 25,
    onMinutesChange: (Int) -> Unit = {},
    state: FocusState = FocusState.IDLE,
    remainingSeconds: Int = 0,
    totalSeconds: Int = 0,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val maxMinutes = 60
    val minMinutes = 5
    val stepMinutes = 5
    
    val displayMinutes = if (state == FocusState.IDLE) selectedMinutes.coerceAtMost(60) else remainingSeconds / 60
    val displaySeconds = if (state == FocusState.IDLE) 0 else remainingSeconds % 60
    
    var currentAngle by remember { mutableStateOf(minutesToAngle60(selectedMinutes.coerceAtMost(60))) }
    
    LaunchedEffect(selectedMinutes) {
        if (state == FocusState.IDLE) {
            currentAngle = minutesToAngle60(selectedMinutes.coerceAtMost(60))
        }
    }
    
    val animatedAngle by animateFloatAsState(
        targetValue = currentAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "angle_animation"
    )
    
    // 🔧 타임타이머 스타일: 남은 시간(분)을 직접 각도로 변환
    // 60분 기준 눈금이므로, 1분 = 6도
    val remainingMinutesFloat = remainingSeconds.toFloat() / 60f
    val runningSweepAngle = remainingMinutesFloat * 6f  // 분을 각도로 변환 (1분 = 6도)
    
    var previousMinutes by remember { mutableStateOf(selectedMinutes) }
    
    val sectorColor = Color(0xFFE53935)
    val sectorColorLight = Color(0xFFFF5252)
    val backgroundColor = Color.White.copy(alpha = 0.95f)
    val tickColor = Color(0xFF424242)
    val numberColor = Color(0xFF616161)

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(260.dp)
                    .then(
                        if (state == FocusState.IDLE) {
                            Modifier
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val angle = calculateAngle60(change.position, center)
                                        val minutes = angleToMinutes60(angle, minMinutes, stepMinutes)
                                        
                                        if (minutes != previousMinutes) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            previousMinutes = minutes
                                        }
                                        
                                        currentAngle = angle
                                        onMinutesChange(minutes)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val angle = calculateAngle60(offset, center)
                                        val minutes = angleToMinutes60(angle, minMinutes, stepMinutes)
                                        
                                        currentAngle = angle
                                        onMinutesChange(minutes)
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                }
                        } else Modifier
                    )
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val outerRadius = size.minDimension / 2
                val innerRadius = outerRadius * 0.35f
                val sectorRadius = outerRadius * 0.85f
                
                // 1. 외곽 눈금 그리기
                for (i in 0 until 60) {
                    val angle = i * 6.0 - 90.0
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
                
                // 2. 부채꼴 그리기
                // IDLE: 설정된 분에 따른 각도
                // RUNNING: 남은 분을 직접 각도로 변환 (60분 기준 눈금과 일치)
                val sweepAngle = if (state == FocusState.IDLE) animatedAngle else runningSweepAngle
                
                if (sweepAngle > 0f) {
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
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - sectorRadius, center.y - sectorRadius),
                        size = Size(sectorRadius * 2, sectorRadius * 2)
                    )
                    
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
                
                // 3. 중앙 흰색 원
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xFFF5F5F5)),
                        center = center,
                        radius = innerRadius
                    ),
                    radius = innerRadius,
                    center = center
                )
                
                drawCircle(
                    color = Color(0xFFE0E0E0),
                    radius = innerRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            
            // 4. 외곽 숫자 표시
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%02d:%02d", displayMinutes, displaySeconds),
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

private fun minutesToAngle60(minutes: Int): Float {
    return (minutes.toFloat() / 60f) * 360f
}

private fun angleToMinutes60(angle: Float, minMinutes: Int, stepMinutes: Int): Int {
    val minutes = (angle / 360f * 60f).roundToInt()
    val coercedMinutes = minutes.coerceIn(minMinutes, 60)
    return (coercedMinutes.toFloat() / stepMinutes).roundToInt() * stepMinutes
}

private fun calculateAngle60(touchPosition: Offset, center: Offset): Float {
    val dx = touchPosition.x - center.x
    val dy = touchPosition.y - center.y
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    angle = (angle + 90) % 360
    if (angle < 0) angle += 360
    return angle
}
