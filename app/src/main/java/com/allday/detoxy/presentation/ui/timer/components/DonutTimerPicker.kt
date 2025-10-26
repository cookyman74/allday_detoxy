package com.allday.detoxy.presentation.ui.timer.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 도넛 그래프 타이머 선택기
 *
 * 사용자가 드래그 또는 탭으로 타이머 시간을 선택할 수 있습니다.
 *
 * ## 주요 기능
 * - 드래그로 시간 조정 (5분 단위)
 * - 탭으로 즉시 시간 설정
 * - 햅틱 피드백 (값 변경 시)
 * - 부드러운 애니메이션
 * - 시간 눈금 표시 (5, 15, 30, 60, 90, 120, 180분)
 *
 * ## 예시
 * ```kotlin
 * var selectedMinutes by remember { mutableStateOf(25) }
 * DonutTimerPicker(
 *     selectedMinutes = selectedMinutes,
 *     onMinutesChange = { selectedMinutes = it }
 * )
 * ```
 *
 * @param selectedMinutes 선택된 시간 (분)
 * @param onMinutesChange 시간 변경 콜백
 * @param minMinutes 최소 시간 (분) - 기본 5분
 * @param maxMinutes 최대 시간 (분) - 기본 180분
 * @param stepMinutes 단위 (분) - 기본 5분
 * @param modifier Modifier
 */
@Composable
fun DonutTimerPicker(
    selectedMinutes: Int,
    onMinutesChange: (Int) -> Unit,
    minMinutes: Int = 5,
    maxMinutes: Int = 180,
    stepMinutes: Int = 5,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val textMeasurer = rememberTextMeasurer()
    
    // 현재 각도 (0~360도)
    var currentAngle by remember { mutableStateOf(minutesToAngle(selectedMinutes, maxMinutes)) }
    
    // 애니메이션 적용된 각도
    val animatedAngle by animateFloatAsState(
        targetValue = currentAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "angle_animation"
    )
    
    // 이전 분 값 (햅틱 피드백 트리거용)
    var previousMinutes by remember { mutableStateOf(selectedMinutes) }
    
    // 색상
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    
    // 텍스트 스타일 (Canvas 외부에서 미리 계산)
    val markerTextStyle = TextStyle(
        fontSize = MaterialTheme.typography.labelSmall.fontSize,
        color = onSurfaceVariant
    )
    
    Box(
        modifier = modifier
            .size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // 드래그 제스처
                    detectDragGestures { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val angle = calculateAngle(change.position, center)
                        val minutes = angleToMinutes(angle, minMinutes, maxMinutes, stepMinutes)
                        
                        if (minutes != previousMinutes) {
                            // 햅틱 피드백 (5분 단위 변경 시)
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            previousMinutes = minutes
                        }
                        
                        currentAngle = angle
                        onMinutesChange(minutes)
                    }
                }
                .pointerInput(Unit) {
                    // 탭 제스처 (즉시 해당 시간 설정)
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val angle = calculateAngle(offset, center)
                        val minutes = angleToMinutes(angle, minMinutes, maxMinutes, stepMinutes)
                        
                        currentAngle = angle
                        onMinutesChange(minutes)
                        
                        // 햅틱 피드백
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 40.dp.toPx()
            val strokeWidth = 24.dp.toPx()
            
            // 1. 배경 원 그리기
            drawCircle(
                color = surfaceVariant,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
            
            // 2. 선택된 영역 arc 그리기
            drawArc(
                color = primaryColor,
                startAngle = -90f, // 12시 방향부터 시작
                sweepAngle = animatedAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // 3. 시간 눈금 그리기
            val markers = listOf(5, 15, 30, 60, 90, 120, 180)
            markers.forEach { markerMinutes ->
                val markerAngle = minutesToAngle(markerMinutes, maxMinutes)
                val angleRad = Math.toRadians((markerAngle - 90).toDouble())
                
                // 눈금 위치 계산
                val markerRadius = radius + 30.dp.toPx()
                val markerX = center.x + (markerRadius * cos(angleRad)).toFloat()
                val markerY = center.y + (markerRadius * sin(angleRad)).toFloat()
                
                // 눈금 점 그리기
                drawCircle(
                    color = onSurfaceVariant.copy(alpha = 0.5f),
                    radius = 4.dp.toPx(),
                    center = Offset(markerX, markerY)
                )
                
                // 눈금 텍스트 그리기
                val textLayoutResult = textMeasurer.measure(
                    text = "${markerMinutes}",
                    style = markerTextStyle
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        markerX - textLayoutResult.size.width / 2,
                        markerY + 12.dp.toPx()
                    )
                )
            }
            
            // 4. 드래그 핸들 그리기
            val handleAngle = animatedAngle - 90 // Canvas 좌표계 변환
            val handleAngleRad = Math.toRadians(handleAngle.toDouble())
            val handleX = center.x + (radius * cos(handleAngleRad)).toFloat()
            val handleY = center.y + (radius * sin(handleAngleRad)).toFloat()
            
            // 외부 원 (흰색)
            drawCircle(
                color = Color.White,
                radius = 16.dp.toPx(),
                center = Offset(handleX, handleY)
            )
            
            // 내부 원 (primary color)
            drawCircle(
                color = primaryColor,
                radius = 12.dp.toPx(),
                center = Offset(handleX, handleY)
            )
        }
        
        // 중앙 텍스트 (선택된 시간)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$selectedMinutes",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )
            Text(
                text = "분",
                style = MaterialTheme.typography.titleMedium,
                color = onSurfaceVariant
            )
        }
    }
}

/**
 * 시간(분) → 각도(도) 변환
 *
 * @param minutes 시간 (분)
 * @param maxMinutes 최대 시간 (분)
 * @return 각도 (0~360도)
 */
private fun minutesToAngle(minutes: Int, maxMinutes: Int): Float {
    return (minutes.toFloat() / maxMinutes) * 360f
}

/**
 * 각도(도) → 시간(분) 변환
 *
 * @param angle 각도 (0~360도)
 * @param minMinutes 최소 시간 (분)
 * @param maxMinutes 최대 시간 (분)
 * @param stepMinutes 단위 (분)
 * @return 시간 (분)
 */
private fun angleToMinutes(angle: Float, minMinutes: Int, maxMinutes: Int, stepMinutes: Int): Int {
    val minutes = (angle / 360f * maxMinutes).roundToInt()
    val coercedMinutes = minutes.coerceIn(minMinutes, maxMinutes)
    return roundToNearestStep(coercedMinutes, stepMinutes)
}

/**
 * 가장 가까운 단위로 반올림
 *
 * @param value 값
 * @param step 단위
 * @return 반올림된 값
 */
private fun roundToNearestStep(value: Int, step: Int): Int {
    return (value.toFloat() / step).roundToInt() * step
}

/**
 * 터치 위치에서 각도 계산
 *
 * @param touchPosition 터치 위치
 * @param center 원의 중심
 * @return 각도 (0~360도, 12시 방향이 0도)
 */
private fun calculateAngle(touchPosition: Offset, center: Offset): Float {
    val dx = touchPosition.x - center.x
    val dy = touchPosition.y - center.y
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    
    // 각도를 0~360 범위로 정규화
    angle = (angle + 90) % 360
    if (angle < 0) angle += 360
    
    return angle
}

