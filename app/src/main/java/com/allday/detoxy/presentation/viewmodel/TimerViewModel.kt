package com.allday.detoxy.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.domain.model.FocusState
import com.allday.detoxy.domain.model.FocusTimer
import com.allday.detoxy.service.accessibility.FocusAccessibilityService
import com.allday.detoxy.service.overlay.LockOverlayService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * 타이머 화면 ViewModel
 *
 * FocusTimer를 관리하고 UI 상태를 제공합니다.
 * AccessibilityService 및 LockOverlayService와 연동하여
 * 앱 차단 및 잠금 화면 기능을 제어합니다.
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    // FocusTimer 인스턴스
    private val focusTimer = FocusTimer(viewModelScope)

    // 타이머 상태
    val timerState: StateFlow<FocusState> = focusTimer.state

    // 남은 시간 (초)
    val remainingSeconds: StateFlow<Int> = focusTimer.remainingSeconds

    // 전체 시간 (초)
    val totalSeconds: StateFlow<Int> = focusTimer.totalSeconds

    // 프리셋 타이머 시간 (분 단위)
    val presetDurations = listOf(25, 45, 60)

    /**
     * 타이머 시작
     *
     * @param durationMinutes 타이머 시간 (분 단위)
     */
    fun startTimer(durationMinutes: Int) {
        // AccessibilityService 활성화
        FocusAccessibilityService.isTimerRunning = true

        // LockOverlayService 시작 (포그라운드 서비스)
        LockOverlayService.showOverlay(
            application,
            remainingSeconds = durationMinutes * 60,
            totalSeconds = durationMinutes * 60
        )

        // 타이머 시작
        focusTimer.start(durationMinutes) { success ->
            onTimerFinish(success)
        }
    }

    /**
     * 타이머 포기
     */
    fun giveUpTimer() {
        // AccessibilityService 비활성화
        FocusAccessibilityService.isTimerRunning = false

        // LockOverlayService 중지
        LockOverlayService.hideOverlay(application)

        // 타이머 포기
        focusTimer.giveUp()
    }

    /**
     * 타이머 리셋
     */
    fun resetTimer() {
        // AccessibilityService 비활성화
        FocusAccessibilityService.isTimerRunning = false

        // LockOverlayService 중지
        LockOverlayService.hideOverlay(application)

        // 타이머 리셋
        focusTimer.reset()
    }

    /**
     * 타이머 완료 콜백
     *
     * @param success 성공 여부
     */
    private fun onTimerFinish(success: Boolean) {
        // AccessibilityService 비활성화
        FocusAccessibilityService.isTimerRunning = false

        // LockOverlayService 중지
        LockOverlayService.hideOverlay(application)

        // TODO: Week 2 - 세션 데이터 저장 (Room DB)
        // TODO: Week 3 - 포인트 지급 및 스트릭 업데이트
    }

    /**
     * 진행률 계산
     */
    fun getProgress(): Float = focusTimer.getProgress()

    /**
     * 포맷된 시간 반환 (MM:SS)
     */
    fun getFormattedTime(): String = focusTimer.getFormattedTime()

    override fun onCleared() {
        super.onCleared()
        // ViewModel 종료 시 타이머도 중지
        if (timerState.value == FocusState.RUNNING) {
            resetTimer()
        }
    }
}
