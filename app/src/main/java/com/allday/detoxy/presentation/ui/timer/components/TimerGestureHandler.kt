package com.allday.detoxy.presentation.ui.timer.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * 타이머 제스처 핸들러 (투명 오버레이)
 * 
 * 원형 드래그 및 탭 제스처를 감지하여 시간을 변경하는 로직을 재사용하기 위한 컴포넌트입니다.
 * 다른 타이머 스타일(Type B, C) 위에 겹쳐서 사용합니다.
 */
@Composable
fun TimerGestureHandler(
    selectedMinutes: Int,
    onMinutesChange: (Int) -> Unit,
    onDragStateChange: (Boolean) -> Unit = {}, // 🆕 드래그 상태 변경 콜백
    minMinutes: Int = 5,
    maxMinutes: Int = 180,
    stepMinutes: Int = 5,
    size: Dp = 240.dp,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    
    Box(
        modifier = modifier
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // 드래그 제스처
                    detectDragGestures(
                        onDragStart = { onDragStateChange(true) }, // 드래그 시작
                        onDragEnd = { onDragStateChange(false) },   // 드래그 종료
                        onDragCancel = { onDragStateChange(false) } // 드래그 취소
                    ) { change, _ ->
                        val center = Offset(this.size.width / 2f, this.size.height / 2f)
                        val angle = calculateAngle(change.position, center)
                        val minutes = angleToMinutes(angle, minMinutes, maxMinutes, stepMinutes)

                        if (minutes != selectedMinutes) {
                            // 햅틱 피드백 (5분 단위 변경 시)
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            onMinutesChange(minutes)
                        }
                    }
                }
                .pointerInput(Unit) {
                    // 탭 제스처
                    detectTapGestures { offset ->
                        val center = Offset(this.size.width / 2f, this.size.height / 2f)
                        val angle = calculateAngle(offset, center)
                        val minutes = angleToMinutes(angle, minMinutes, maxMinutes, stepMinutes)

                        onMinutesChange(minutes)
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    }
                }
        )
    }
}

/**
 * 터치 위치에서 각도 계산 (12시 기준 0~360도)
 */
private fun calculateAngle(touchPosition: Offset, center: Offset): Float {
    val dx = touchPosition.x - center.x
    val dy = touchPosition.y - center.y
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    
    // 각도를 0~360 범위로 정규화 (12시 방향이 0도가 되도록 +90)
    angle = (angle + 90) % 360
    if (angle < 0) angle += 360
    
    return angle
}

/**
 * 각도(도) → 시간(분) 변환
 */
private fun angleToMinutes(angle: Float, minMinutes: Int, maxMinutes: Int, stepMinutes: Int): Int {
    val minutes = (angle / 360f * maxMinutes).roundToInt()
    val coercedMinutes = minutes.coerceIn(minMinutes, maxMinutes)
    return roundToNearestStep(coercedMinutes, stepMinutes)
}

/**
 * 가장 가까운 단위로 반올림
 */
private fun roundToNearestStep(value: Int, step: Int): Int {
    return (value.toFloat() / step).roundToInt() * step
}
