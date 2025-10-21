package com.allday.detoxy.presentation.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.core.manager.DndManager
import com.allday.detoxy.data.local.entity.FocusSession
import com.allday.detoxy.data.local.entity.UserSettings
import com.allday.detoxy.domain.manager.GamificationManager
import com.allday.detoxy.core.utils.PermissionUtils
import com.allday.detoxy.domain.model.FocusState
import com.allday.detoxy.domain.repository.FocusRepository
import com.allday.detoxy.domain.repository.FocusSettingsRepository
import com.allday.detoxy.service.accessibility.FocusAccessibilityService
import com.allday.detoxy.service.overlay.LockOverlayService
import com.allday.detoxy.service.timer.FocusTimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 타이머 화면 ViewModel
 *
 * FocusTimerService를 관리하고 UI 상태를 제공합니다.
 * AccessibilityService, DndManager와 연동하여
 * 앱 차단, 방해금지 모드 기능을 제어합니다.
 *
 * Week 3: FocusRepository와 GamificationManager를 통해
 * 세션 저장 및 포인트/스트릭 업데이트 기능을 제공합니다.
 * 
 * 버그 수정 (2025-10-20): Foreground Service로 타이머 관리하여
 * 화면 슬립 모드에서도 타이머가 정상 작동하도록 개선
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val application: Application,
    private val repository: FocusRepository,
    private val settingsRepository: FocusSettingsRepository,
    private val gamificationManager: GamificationManager
) : ViewModel() {

    // DndManager 인스턴스
    private val dndManager = DndManager(application)

    // 타이머 상태 (FocusTimerService에서 가져옴)
    val timerState: StateFlow<FocusState> = FocusTimerService.state

    // 남은 시간 (초) (FocusTimerService에서 가져옴)
    val remainingSeconds: StateFlow<Int> = FocusTimerService.remainingSeconds

    // 전체 시간 (초) (FocusTimerService에서 가져옴)
    val totalSeconds: StateFlow<Int> = FocusTimerService.totalSeconds

    // 프리셋 타이머 시간 (분 단위)
    val presetDurations = listOf(25, 45, 60)

    // 현재 세션 ID (타이머 시작 시 생성)
    private var currentSessionId: String? = null

    // 권한 에러 이벤트
    private val _permissionError = MutableStateFlow<PermissionError?>(null)
    val permissionError: StateFlow<PermissionError?> = _permissionError.asStateFlow()

    // 타이머 완료 추적을 위한 이전 상태
    private var previousTimerState: FocusState = FocusState.IDLE

    companion object {
        private const val TAG = "TimerViewModel"
    }

    /**
     * 권한 에러 타입
     */
    sealed class PermissionError {
        object AccessibilityServiceDisabled : PermissionError()
        object OverlayPermissionDenied : PermissionError()
    }

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

        // 타이머 상태 관찰 (브로드캐스트 대신 StateFlow 사용)
        viewModelScope.launch {
            timerState.collect { currentState ->
                Log.d(TAG, "🔔 Timer state changed: $previousTimerState → $currentState")
                
                // RUNNING → FINISHED: 정상 완료
                if (previousTimerState == FocusState.RUNNING && currentState == FocusState.FINISHED) {
                    Log.d(TAG, "✅ Timer finished successfully (detected via StateFlow)")
                    onTimerFinish(success = true)
                }
                // RUNNING → FAILED: 포기
                else if (previousTimerState == FocusState.RUNNING && currentState == FocusState.FAILED) {
                    Log.d(TAG, "❌ Timer failed (detected via StateFlow)")
                    onTimerFinish(success = false)
                }
                
                previousTimerState = currentState
            }
        }

        // 타이머 남은 시간을 AccessibilityService에 실시간 동기화
        // (크롬 실행 시 정확한 남은 시간을 LockOverlayScreen에 전달하기 위함)
        viewModelScope.launch {
            remainingSeconds.collect { seconds ->
                FocusAccessibilityService.remainingSeconds = seconds
            }
        }
        
        Log.d(TAG, "✅ TimerViewModel initialized with StateFlow observation")
    }

    /**
     * 타이머 시작
     *
     * @param durationMinutes 타이머 시간 (분 단위)
     */
    fun startTimer(durationMinutes: Int) {
        Log.d(TAG, "Starting timer: $durationMinutes minutes")

        // 0. 필수 권한 확인
        if (!PermissionUtils.isAccessibilityServiceEnabled(application)) {
            Log.e(TAG, "❌ 접근성 서비스가 비활성화되어 있습니다")
            _permissionError.value = PermissionError.AccessibilityServiceDisabled
            return
        }

        if (!PermissionUtils.canDrawOverlays(application)) {
            Log.e(TAG, "❌ 오버레이 권한이 없습니다")
            _permissionError.value = PermissionError.OverlayPermissionDenied
            return
        }

        Log.d(TAG, "✅ All required permissions granted")

        // 1. 세션 생성 및 저장 (Week 3)
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

            // 1.5. 디톡시 제어 설정 로드 및 AccessibilityService에 전달 (1차 고도화)
            val (categories, otherApps) = settingsRepository.getCurrentSettings()
            FocusAccessibilityService.updateBlockSettings(categories, otherApps)
            Log.i(TAG, "✅ Block settings loaded: ${categories.size} categories, otherApps=$otherApps")
        }

        // 2. AccessibilityService 활성화 및 타이머 정보 전달
        val totalSec = durationMinutes * 60
        FocusAccessibilityService.isTimerRunning = true
        FocusAccessibilityService.remainingSeconds = totalSec
        FocusAccessibilityService.totalSeconds = totalSec
        FocusAccessibilityService.currentSessionId = sessionId  // 차단 이벤트 로깅용 (v2)

        // 3. DND 모드 활성화 (Android 6.0 이상, 권한 있을 경우만)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dndManager.enableDnd()
        }

        // 4. FocusTimerService 시작 (Foreground Service)
        FocusTimerService.startTimer(application, durationMinutes, sessionId)
        Log.d(TAG, "✅ FocusTimerService started")
    }

    /**
     * 타이머 포기 (v2: interruptedSeconds 기록)
     */
    fun giveUpTimer() {
        // 0. 경과 시간 계산 (목표 시간 - 남은 시간)
        val elapsedSeconds = totalSeconds.value - remainingSeconds.value
        val endTime = System.currentTimeMillis()
        
        Log.d(TAG, "🛑 Timer give up - elapsed: ${elapsedSeconds}s")

        // 0-1. LockOverlayService 명시적 종료
        LockOverlayService.hideOverlay(application)

        // 0-2. 세션 종료 처리 (Week 3 → v2 확장)
        currentSessionId?.let { sessionId ->
            viewModelScope.launch {
                // 경과 시간과 포기 사유를 포함하여 세션 종료
                repository.endSessionWithDetails(
                    sessionId = sessionId,
                    success = false,
                    endTime = endTime,
                    interruptedSeconds = elapsedSeconds,
                    giveUpReason = "user_give_up"
                )
                
                Log.i(TAG, "✅ Session ended with details: $elapsedSeconds seconds elapsed")
                
                currentSessionId = null
            }
        }

        // 1. AccessibilityService 비활성화 및 모든 상태 초기화
        FocusAccessibilityService.isTimerRunning = false
        FocusAccessibilityService.remainingSeconds = 0
        FocusAccessibilityService.totalSeconds = 0
        FocusAccessibilityService.currentSessionId = null

        // 2. DND 모드 비활성화
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dndManager.disableDnd()
        }

        // 3. FocusTimerService 포기
        FocusTimerService.giveUpTimer(application)
        Log.d(TAG, "✅ FocusTimerService give up called and all services cleaned up")
    }

    /**
     * 타이머 리셋
     */
    fun resetTimer() {
        // 0. LockOverlayService 명시적 종료
        LockOverlayService.hideOverlay(application)
        
        // 1. 세션 ID 초기화 (Week 3)
        currentSessionId = null

        // 2. AccessibilityService 비활성화 및 모든 상태 초기화
        FocusAccessibilityService.isTimerRunning = false
        FocusAccessibilityService.remainingSeconds = 0
        FocusAccessibilityService.totalSeconds = 0
        FocusAccessibilityService.currentSessionId = null

        // 3. DND 모드 비활성화
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dndManager.disableDnd()
        }

        // 4. FocusTimerService 중지
        FocusTimerService.stopTimer(application)
        Log.d(TAG, "✅ FocusTimerService stopped and all services cleaned up")
    }

    /**
     * 타이머 완료 콜백
     *
     * @param success 성공 여부
     */
    private fun onTimerFinish(success: Boolean) {
        Log.d(TAG, "⏰ Timer finished callback: success=$success")
        
        // 1. LockOverlayService 명시적 종료 (오버레이 깜빡임 방지)
        LockOverlayService.hideOverlay(application)
        Log.d(TAG, "✅ LockOverlayService hideOverlay called")
        
        // 2. AccessibilityService 비활성화 및 모든 상태 초기화
        FocusAccessibilityService.isTimerRunning = false
        FocusAccessibilityService.remainingSeconds = 0
        FocusAccessibilityService.totalSeconds = 0
        FocusAccessibilityService.currentSessionId = null
        Log.d(TAG, "✅ AccessibilityService state cleared")

        // 3. DND 모드 비활성화
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dndManager.disableDnd()
            Log.d(TAG, "✅ DND mode disabled")
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
                Log.d(TAG, "✅ Session ended: sessionId=$sessionId, success=$success")

                // 성공 시 포인트 지급 및 스트릭 업데이트
                if (success) {
                    val settings = repository.getSettings().first()
                    settings?.let {
                        // 포인트 계산 및 추가
                        val session = repository.getSession(sessionId).first()
                        session?.let { focusSession ->
                            val points = gamificationManager.calculatePoints(focusSession.durationMinutes)
                            repository.addPoints(points)
                            Log.d(TAG, "✅ Points added: $points")

                            // 스트릭 업데이트
                            val updatedSettings = gamificationManager.updateStreak(it, success)
                            repository.updateStreak(updatedSettings.currentStreak, updatedSettings.lastSuccessDate ?: "")
                            Log.d(TAG, "✅ Streak updated: ${updatedSettings.currentStreak}")
                        }
                    }
                } else {
                    Log.d(TAG, "⚠️ Session failed, no points or streak update")
                }

                currentSessionId = null
                Log.d(TAG, "✅ Timer finish processing completed")
            }
        } ?: Log.w(TAG, "⚠️ currentSessionId is null, cannot end session")
    }

    /**
     * 권한 에러 초기화
     */
    fun clearPermissionError() {
        _permissionError.value = null
    }

    /**
     * 진행률 계산
     */
    fun getProgress(): Float {
        if (totalSeconds.value == 0) return 0f
        val elapsed = totalSeconds.value - remainingSeconds.value
        return elapsed.toFloat() / totalSeconds.value.toFloat()
    }

    /**
     * 포맷된 시간 반환 (MM:SS)
     */
    fun getFormattedTime(): String {
        val minutes = remainingSeconds.value / 60
        val seconds = remainingSeconds.value % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onCleared() {
        super.onCleared()
        
        // ViewModel 종료 시 타이머도 중지
        if (timerState.value == FocusState.RUNNING) {
            resetTimer()
        }
        
        Log.d(TAG, "✅ TimerViewModel cleared")
    }
}
