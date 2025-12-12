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
 * Type A: Liquid Ring (Interactive)
 * 
 * 드래그 가능한 포인터가 있는 링 타이머입니다.
 * IDLE 상태에서는 시간 선택, RUNNING 상태에서는 진행률 표시
 *
 * @param selectedMinutes 선택된 시간 (분) - IDLE 상태용
 * @param onMinutesChange 시간 변경 콜백 - IDLE 상태용
 * @param state 타이머 상태
 * @param remainingSeconds 남은 시간 (초) - RUNNING 상태용
 * @param totalSeconds 전체 시간 (초) - RUNNING 상태용
 * @param modifier Modifier
 */
@Composable
fun TimerStyleLiquidRing(
    selectedMinutes: Int = 25,
    onMinutesChange: (Int) -> Unit = {},
    state: FocusState = FocusState.IDLE,
    remainingSeconds: Int = 0,
    totalSeconds: Int = 0,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val maxMinutes = 180
    val minMinutes = 5
    val stepMinutes = 5
    
    // IDLE 상태: selectedMinutes 사용, RUNNING 상태: remainingSeconds 사용
    val displayMinutes = if (state == FocusState.IDLE) selectedMinutes else remainingSeconds / 60
    val displaySeconds = if (state == FocusState.IDLE) 0 else remainingSeconds % 60
    
    // 현재 각도 (드래그용)
    var currentAngle by remember { mutableStateOf(minutesToAngle(selectedMinutes, maxMinutes)) }
    
    // selectedMinutes가 외부에서 변경되면 currentAngle 업데이트
    LaunchedEffect(selectedMinutes) {
        if (state == FocusState.IDLE) {
            currentAngle = minutesToAngle(selectedMinutes, maxMinutes)
        }
    }
    
    // 애니메이션 적용된 각도
    val animatedAngle by animateFloatAsState(
        targetValue = currentAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "angle_animation"
    )
    
    // 진행률 (RUNNING 상태용)
    val progress = remember(remainingSeconds, totalSeconds) {
        if (totalSeconds == 0) 0f
        else remainingSeconds.toFloat() / totalSeconds.toFloat()
    }
    
    // 이전 분 값 (햅틱 피드백용)
    var previousMinutes by remember { mutableStateOf(selectedMinutes) }
    
    // 색상
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = Color.White.copy(alpha = 0.95f)
    val trackColor = Color(0xFFE0E0E0)
    val tickColor = Color(0xFF9E9E9E)

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // 외부 프레임
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
                                        val angle = calculateAngle(change.position, center)
                                        val minutes = angleToMinutes(angle, minMinutes, maxMinutes, stepMinutes)
                                        
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
                                        val angle = calculateAngle(offset, center)
                                        val minutes = angleToMinutes(angle, minMinutes, maxMinutes, stepMinutes)
                                        
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
                
                // 3. 진행률 링
                val sweepAngle = if (state == FocusState.IDLE) animatedAngle else 360f * progress
                if (sweepAngle > 0f) {
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                        size = Size(ringRadius * 2, ringRadius * 2),
                        style = Stroke(width = ringWidth, cap = StrokeCap.Round)
                    )
                    
                    // 4. 드래그 핸들 (포인터)
                    val endAngleRad = Math.toRadians(-90.0 + sweepAngle)
                    val endX = center.x + ringRadius * cos(endAngleRad).toFloat()
                    val endY = center.y + ringRadius * sin(endAngleRad).toFloat()
                    
                    // 외부 원 (흰색)
                    drawCircle(
                        color = Color.White,
                        radius = 16.dp.toPx(),
                        center = Offset(endX, endY)
                    )
                    // 내부 원 (Primary)
                    drawCircle(
                        color = primaryColor,
                        radius = 12.dp.toPx(),
                        center = Offset(endX, endY)
                    )
                    // 중심 점 (흰색)
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(endX, endY)
                    )
                }
            }
            
            // 5. 외곽 숫자 표시
            Box(modifier = Modifier.size(260.dp)) {
                val numbers = listOf(5, 25, 45, 70, 90, 115, 135, 160)
                numbers.forEach { num ->
                    val angle = (num.toFloat() / maxMinutes) * 360f - 90f
                    val angleRad = Math.toRadians(angle.toDouble())
                    val numberRadius = 105.dp
                    
                    Text(
                        text = num.toString(),
                        fontSize = 11.sp,
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
            
            // 6. 중앙 시간 텍스트
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (state == FocusState.IDLE) "$displayMinutes" else String.format("%02d:%02d", displayMinutes, displaySeconds),
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

// 유틸리티 함수들
private fun minutesToAngle(minutes: Int, maxMinutes: Int): Float {
    return (minutes.toFloat() / maxMinutes) * 360f
}

private fun angleToMinutes(angle: Float, minMinutes: Int, maxMinutes: Int, stepMinutes: Int): Int {
    val minutes = (angle / 360f * maxMinutes).roundToInt()
    val coercedMinutes = minutes.coerceIn(minMinutes, maxMinutes)
    return (coercedMinutes.toFloat() / stepMinutes).roundToInt() * stepMinutes
}

private fun calculateAngle(touchPosition: Offset, center: Offset): Float {
    val dx = touchPosition.x - center.x
    val dy = touchPosition.y - center.y
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    angle = (angle + 90) % 360
    if (angle < 0) angle += 360
    return angle
}
