package com.allday.detoxy.presentation.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.core.manager.DndManager
import com.allday.detoxy.data.local.entity.FocusSession
import com.allday.detoxy.data.local.entity.UserSettings
import com.allday.detoxy.domain.manager.GamificationManager
import com.allday.detoxy.domain.model.FocusState
import com.allday.detoxy.domain.model.FocusTimer
import com.allday.detoxy.domain.repository.FocusRepository
import com.allday.detoxy.service.accessibility.FocusAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 타이머 화면 ViewModel
 *
 * FocusTimer를 관리하고 UI 상태를 제공합니다.
 * AccessibilityService, DndManager와 연동하여
 * 앱 차단, 방해금지 모드 기능을 제어합니다.
 *
 * Week 3: FocusRepository와 GamificationManager를 통해
 * 세션 저장 및 포인트/스트릭 업데이트 기능을 제공합니다.
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val application: Application,
    private val repository: FocusRepository,
    private val gamificationManager: GamificationManager
) : ViewModel() {

    // FocusTimer 인스턴스
    private val focusTimer = FocusTimer(viewModelScope)

    // DndManager 인스턴스
    private val dndManager = DndManager(application)

    // 타이머 상태
    val timerState: StateFlow<FocusState> = focusTimer.state

    // 남은 시간 (초)
    val remainingSeconds: StateFlow<Int> = focusTimer.remainingSeconds

    // 전체 시간 (초)
    val totalSeconds: StateFlow<Int> = focusTimer.totalSeconds

    // 프리셋 타이머 시간 (분 단위)
    val presetDurations = listOf(25, 45, 60)

    // 현재 세션 ID (타이머 시작 시 생성)
    private var currentSessionId: String? = null

    init {
        // 사용자 설정 초기화 (최초 실행 시)
        viewModelScope.launch {
            val settings = repository.getSettings().first()
            if (settings == null) {
                repository.initializeSettings(
                    UserSettings(
                        id = 1,
                        totalPoints = 0,
                        currentStreak = 0,
                        lastSuccessDate = null
                    )
                )
            }
        }

        // 타이머 남은 시간을 AccessibilityService에 실시간 동기화
        // (크롬 실행 시 정확한 남은 시간을 LockOverlayScreen에 전달하기 위함)
        viewModelScope.launch {
            remainingSeconds.collect { seconds ->
                FocusAccessibilityService.remainingSeconds = seconds
            }
        }
    }

    /**
     * 타이머 시작
     *
     * @param durationMinutes 타이머 시간 (분 단위)
     */
    fun startTimer(durationMinutes: Int) {
        // 0. 세션 생성 및 저장 (Week 3)
        val sessionId = UUID.randomUUID().toString()
        currentSessionId = sessionId
        viewModelScope.launch {
            repository.startSession(
                FocusSession(
                    id = sessionId,
                    startTime = System.currentTimeMillis(),
                    endTime = null,
                    durationMinutes = durationMinutes,
                    success = false
                )
            )
        }

        // 1. AccessibilityService 활성화 및 타이머 정보 전달
        val totalSec = durationMinutes * 60
        FocusAccessibilityService.isTimerRunning = true
        FocusAccessibilityService.remainingSeconds = totalSec
        FocusAccessibilityService.totalSeconds = totalSec

        // 2. DND 모드 활성화 (Android 6.0 이상, 권한 있을 경우만)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dndManager.enableDnd()
        }

        // 3. 타이머 시작
        focusTimer.start(durationMinutes) { success ->
            onTimerFinish(success)
        }
    }

    /**
     * 타이머 포기
     */
    fun giveUpTimer() {
        // 0. 세션 종료 처리 (Week 3)
        currentSessionId?.let { sessionId ->
            viewModelScope.launch {
                repository.endSession(
                    sessionId = sessionId,
                    success = false,
                    endTime = System.currentTimeMillis()
                )
                currentSessionId = null
            }
        }

        // 1. AccessibilityService 비활성화
        FocusAccessibilityService.isTimerRunning = false
        FocusAccessibilityService.remainingSeconds = 0
        FocusAccessibilityService.totalSeconds = 0

        // 2. DND 모드 비활성화
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dndManager.disableDnd()
        }

        // 3. 타이머 포기
        focusTimer.giveUp()
    }

    /**
     * 타이머 리셋
     */
    fun resetTimer() {
        // 0. 세션 ID 초기화 (Week 3)
        currentSessionId = null

        // 1. AccessibilityService 비활성화
        FocusAccessibilityService.isTimerRunning = false

        // 2. DND 모드 비활성화
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dndManager.disableDnd()
        }

        // 3. 타이머 리셋
        focusTimer.reset()
    }

    /**
     * 타이머 완료 콜백
     *
     * @param success 성공 여부
     */
    private fun onTimerFinish(success: Boolean) {
        // 1. AccessibilityService 비활성화
        FocusAccessibilityService.isTimerRunning = false

        // 2. DND 모드 비활성화
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dndManager.disableDnd()
        }

        // 4. 세션 종료 및 포인트/스트릭 업데이트 (Week 3)
        currentSessionId?.let { sessionId ->
            viewModelScope.launch {
                // 세션 종료
                repository.endSession(
                    sessionId = sessionId,
                    success = success,
                    endTime = System.currentTimeMillis()
                )

                // 성공 시 포인트 지급 및 스트릭 업데이트
                if (success) {
                    val settings = repository.getSettings().first()
                    settings?.let {
                        // 포인트 계산 및 추가
                        val session = repository.getSession(sessionId).first()
                        session?.let { focusSession ->
                            val points = gamificationManager.calculatePoints(focusSession.durationMinutes)
                            repository.addPoints(points)

                            // 스트릭 업데이트
                            val updatedSettings = gamificationManager.updateStreak(it, success)
                            repository.updateStreak(updatedSettings.currentStreak, updatedSettings.lastSuccessDate ?: "")
                        }
                    }
                }

                currentSessionId = null
            }
        }
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
