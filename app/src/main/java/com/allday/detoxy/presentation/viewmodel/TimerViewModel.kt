package com.allday.detoxy.presentation.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.core.manager.DndManager
import com.allday.detoxy.core.utils.PreferenceManager
import com.allday.detoxy.data.local.converter.ScheduleInfoConverter
import com.allday.detoxy.data.local.dao.FocusSessionTodoResultDao
import com.allday.detoxy.data.local.dao.TimeBasedAutoRunDao
import com.allday.detoxy.data.local.entity.FocusSession
import com.allday.detoxy.data.local.entity.FocusSessionTodoResultEntity
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.data.local.entity.UserSettings
import com.allday.detoxy.domain.manager.GamificationManager
import com.allday.detoxy.core.utils.PermissionUtils
import com.allday.detoxy.domain.model.FocusState
import com.allday.detoxy.domain.model.ScheduleInfo
import com.allday.detoxy.domain.model.ScheduleType
import com.allday.detoxy.domain.model.TodoCompletionStatus
import com.allday.detoxy.domain.repository.FocusRepository
import com.allday.detoxy.domain.repository.FocusSettingsRepository
import com.allday.detoxy.domain.util.SessionEndDialogType
import com.allday.detoxy.domain.util.TodoResultBuilder
import com.allday.detoxy.domain.util.resolveSessionEndDialogType
import com.allday.detoxy.service.accessibility.FocusAccessibilityService
import com.allday.detoxy.service.overlay.LockOverlayService
import com.allday.detoxy.service.timer.FocusTimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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
    private val gamificationManager: GamificationManager,
    private val timeBasedAutoRunDao: TimeBasedAutoRunDao,
    private val presetRepository: com.allday.detoxy.data.repository.CustomTimerPresetRepository,
    private val todoResultDao: FocusSessionTodoResultDao  // 🆕 v9
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

    // 다음 예약 정보 (UI 표시용)
    val nextAutoRunInfo: StateFlow<String?> = timeBasedAutoRunDao.getEnabled()
        .map { autoRuns ->
            calculateNextAutoRunInfo(autoRuns)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // 커스텀 프리셋 목록
    val customPresets: StateFlow<List<com.allday.detoxy.data.local.entity.CustomTimerPreset>> = 
        presetRepository.getAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    companion object {
        private const val TAG = "TimerViewModel"
    }

    /**
     * 권한 에러 타입
     */
    sealed class PermissionError {
        object AccessibilityServiceDisabled : PermissionError()
        object AccessibilityServiceCrashed : PermissionError()  // 🆕 v0.10.4: 크래시 상태
        object OverlayPermissionDenied : PermissionError()
    }

    // 🆕 성공 애니메이션 표시 상태 (init 블록보다 먼저 선언되어야 함)
    // 🆕 성공 애니메이션 표시 상태 (init 블록보다 먼저 선언되어야 함)
    private val _showSuccessAnimation = MutableStateFlow(false)
    val showSuccessAnimation: StateFlow<Boolean> = _showSuccessAnimation.asStateFlow()

    // 🆕 마지막 선택된 타이머 스타일 인덱스 (0: LiquidRing, 1: MinimalFlux, 2: GlassSector)
    private val _selectedTimerStyleIndex = MutableStateFlow(0)
    val selectedTimerStyleIndex: StateFlow<Int> = _selectedTimerStyleIndex.asStateFlow()
    
    // 🆕 v9: 세션 종료 다이얼로그 상태
    private val _showTodoDialog = MutableStateFlow(false)
    val showTodoDialog: StateFlow<Boolean> = _showTodoDialog.asStateFlow()
    
    private val _showGoalDialog = MutableStateFlow(false)
    val showGoalDialog: StateFlow<Boolean> = _showGoalDialog.asStateFlow()
    
    private val _currentScheduleInfo = MutableStateFlow<ScheduleInfo?>(null)
    val currentScheduleInfo: StateFlow<ScheduleInfo?> = _currentScheduleInfo.asStateFlow()
    
    // 🆕 v9: 세션 메타데이터 (리뷰 피드백 반영)
    // 세션 시작 시 저장하여 종료 시 안정적으로 술
    private var currentScheduleGroupId: String? = null
    private var currentScheduleType: ScheduleType = ScheduleType.TIME_BASED
    
    private val scheduleInfoConverter = ScheduleInfoConverter()

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
                Log.d(TAG, "   - previousTimerState: $previousTimerState")
                Log.d(TAG, "   - currentState: $currentState")
                
                // RUNNING → FINISHED: 정상 완료
                if (previousTimerState == FocusState.RUNNING && currentState == FocusState.FINISHED) {
                    Log.d(TAG, "✅ Timer finished successfully (detected via StateFlow) - will call onTimerFinish(success = true)")
                    onTimerFinish(success = true)
                }
                // RUNNING → FAILED: 포기
                else if (previousTimerState == FocusState.RUNNING && currentState == FocusState.FAILED) {
                    Log.d(TAG, "❌ Timer failed (detected via StateFlow) - will call onTimerFinish(success = false)")
                    onTimerFinish(success = false)
                }
                // 다른 상태 전환은 무시 (예: IDLE → RUNNING)
                else {
                    Log.d(TAG, "ℹ️ State transition ignored: $previousTimerState → $currentState")
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

        // 🆕 FocusTimerService의 currentSessionId를 관찰하여 동기화
        viewModelScope.launch {
            FocusTimerService.currentSessionId.collect { serviceSessionId ->
                if (serviceSessionId != null && currentSessionId != serviceSessionId) {
                    Log.d(TAG, "🔄 Syncing currentSessionId from FocusTimerService: $serviceSessionId")
                    currentSessionId = serviceSessionId
                }
            }
        }

        // 🆕 앱 실행 시 성공 애니메이션 대기 상태 확인 (v0.10.2)
        checkPendingSuccessAnimation()
        
        Log.d(TAG, "✅ TimerViewModel initialized with StateFlow observation")
    }

    /**
     * 성공 애니메이션 대기 상태 확인
     */
    /**
     * 성공 애니메이션 대기 상태 확인
     */
    fun checkPendingSuccessAnimation() {
        val preferenceManager = com.allday.detoxy.core.utils.PreferenceManager(application)
        if (preferenceManager.hasPendingSuccessAnimation()) {
            Log.d(TAG, "🎉 Pending success animation detected!")
            _showSuccessAnimation.value = true
        }
        
        // 🆕 저장된 타이머 스타일 인덱스 로드
        viewModelScope.launch {
            val savedIndex = preferenceManager.getLastTimerStyleIndex()
            _selectedTimerStyleIndex.value = savedIndex
            Log.d(TAG, "🔄 Loaded timer style index: $savedIndex")
        }
    }

    /**
     * 성공 애니메이션 표시 완료 처리
     */
    fun onSuccessAnimationShown() {
        Log.d(TAG, "✅ Success animation shown, clearing flag")
        val preferenceManager = com.allday.detoxy.core.utils.PreferenceManager(application)
        preferenceManager.setPendingSuccessAnimation(false)
        _showSuccessAnimation.value = false
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
        
        // 🆕 v0.10.4: 접근성 서비스 크래시 상태 확인
        if (PermissionUtils.isAccessibilityServiceCrashed(application)) {
            Log.e(TAG, "⚠️ 접근성 서비스가 크래시 상태입니다. 재시작이 필요합니다.")
            _permissionError.value = PermissionError.AccessibilityServiceCrashed
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
        }

        // 2. FocusTimerService 시작 (Foreground Service)
        // ⚠️ FocusTimerService.startTimerInternal()에서 모든 설정을 처리하도록 일원화
        // - AccessibilityService 설정 로드
        // - isTimerRunning, remainingSeconds, totalSeconds, currentSessionId 설정
        // - DND 모드 활성화
        FocusTimerService.startTimer(application, durationMinutes, sessionId)
        Log.d(TAG, "✅ FocusTimerService started (sessionId=$sessionId)")
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
        Log.d(TAG, "🔍 Current sessionId: $currentSessionId")
        
        // 🆕 currentSessionId가 null인 경우 FocusTimerService에서 가져오기 시도
        val sessionIdToUse = currentSessionId ?: run {
            val serviceSessionId = FocusTimerService.currentSessionId.value
            Log.d(TAG, "⚠️ currentSessionId is null, trying FocusTimerService.currentSessionId: $serviceSessionId")
            if (serviceSessionId != null) {
                currentSessionId = serviceSessionId
            }
            serviceSessionId
        }
        
        // 1. LockOverlayService 명시적 종료 (오버레이 깜빡임 방지)
        LockOverlayService.hideOverlay(application)
        Log.d(TAG, "✅ LockOverlayService hideOverlay called")
        
        // 2. AccessibilityService 비활성화 및 모든 상태 초기화
        FocusAccessibilityService.isTimerRunning = false
        FocusAccessibilityService.remainingSeconds = 0
        FocusAccessibilityService.totalSeconds = 0
        
        // 🆕 v9: 스케줄 정보 저장 (다이얼로그 표시용)
        val scheduleInfoJson = FocusAccessibilityService.currentScheduleInfoJson
        _currentScheduleInfo.value = scheduleInfoJson?.let { scheduleInfoConverter.toScheduleInfo(it) }
        Log.d(TAG, "🎯 Current schedule info: ${_currentScheduleInfo.value}")
        
        // 🆕 v9 리뷰 피드백: 세션 메타데이터 저장 (종료 시점에 읽으면 null일 수 있음)
        currentScheduleGroupId = FocusTimerService.currentScheduleGroupId.value
        currentScheduleType = FocusTimerService.currentScheduleType.value ?: ScheduleType.TIME_BASED
        Log.d(TAG, "📦 Saved schedule metadata: groupId=$currentScheduleGroupId, type=$currentScheduleType")
        
        FocusAccessibilityService.currentSessionId = null
        FocusAccessibilityService.currentScheduleInfoJson = null
        Log.d(TAG, "✅ AccessibilityService state cleared")
        
        // 3. DND 모드 비활성화
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dndManager.disableDnd()
            Log.d(TAG, "✅ DND mode disabled")
        }

        // 4. 세션 종료 처리
        sessionIdToUse?.let { sessionId ->
            viewModelScope.launch {
                // 세션 종료
                Log.d(TAG, "💾 Ending session: sessionId=$sessionId, success=$success")
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
                    
                    // 🆕 v9: 세션 종료 다이얼로그 표시 여부 결정
                    showSessionEndDialog()
                } else {
                    Log.d(TAG, "⚠️ Session failed, no points or streak update")
                    currentSessionId = null
                }
                
                Log.d(TAG, "✅ Timer finish processing completed")
            }
        } ?: run {
            // 🔥 버그 수정: sessionId가 null인 경우 최근 세션을 찾아서 처리
            Log.w(TAG, "⚠️ sessionId is null, trying to find recent session from database")
            viewModelScope.launch {
                try {
                    // 최근 5분 내에 시작된 세션 중 endTime이 null인 세션 찾기
                    val now = System.currentTimeMillis()
                    val fiveMinutesAgo = now - (5 * 60 * 1000)
                    val recentSessions = repository.getSessionsInRange(fiveMinutesAgo, now)
                    val activeSession = recentSessions.firstOrNull { it.endTime == null }
                    
                    if (activeSession != null) {
                        Log.i(TAG, "✅ Found active session: sessionId=${activeSession.id}, duration=${activeSession.durationMinutes}min")
                        // 세션 종료
                        repository.endSession(
                            sessionId = activeSession.id,
                            success = success,
                            endTime = now
                        )
                        Log.d(TAG, "✅ Session ended (fallback): sessionId=${activeSession.id}, success=$success")
                        
                        // 성공 시 포인트 지급 및 스트릭 업데이트
                        if (success) {
                            val settings = repository.getSettings().first()
                            settings?.let {
                                val points = gamificationManager.calculatePoints(activeSession.durationMinutes)
                                repository.addPoints(points)
                                Log.d(TAG, "✅ Points added (fallback): $points")
                                
                                val updatedSettings = gamificationManager.updateStreak(it, success)
                                repository.updateStreak(updatedSettings.currentStreak, updatedSettings.lastSuccessDate ?: "")
                                Log.d(TAG, "✅ Streak updated (fallback): ${updatedSettings.currentStreak}")
                            }
                            
                            // 🆕 v9: 세션 종료 다이얼로그 표시
                            showSessionEndDialog()
                        }
                    } else {
                        Log.e(TAG, "❌ No active session found in recent 5 minutes. Cannot end session.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to find and end session (fallback): ${e.message}", e)
                }
            }
        }
    }
    
    /**
     * 🆕 v9: 세션 종료 다이얼로그 표시 여부 결정
     */
    private fun showSessionEndDialog() {
        val scheduleInfo = _currentScheduleInfo.value
        val dialogType = resolveSessionEndDialogType(scheduleInfo)
        
        Log.d(TAG, "🎯 Session end dialog type: $dialogType")
        
        when (dialogType) {
            is SessionEndDialogType.Skip -> {
                // 다이얼로그 없이 바로 종료 (리뷰 피드백: _currentScheduleInfo도 초기화)
                Log.d(TAG, "📝 No dialog needed, finishing session")
                currentSessionId = null
                _currentScheduleInfo.value = null
                currentScheduleGroupId = null
            }
            is SessionEndDialogType.GoalOnly -> {
                _showGoalDialog.value = true
            }
            is SessionEndDialogType.TodoChecklist -> {
                _showTodoDialog.value = true
            }
        }
    }
    
    /**
     * 🆕 v9: 할일 체크 다이얼로그 완료 콜백
     */
    fun onTodoDialogComplete(responses: Map<String, TodoCompletionStatus>) {
        _showTodoDialog.value = false
        
        val scheduleInfo = _currentScheduleInfo.value ?: return
        val sessionId = currentSessionId
        
        viewModelScope.launch {
            try {
                // 리뷰 피드백: 세션 시작 시 저장한 값 사용 (종료 시점에 읽으면 null일 수 있음)
                val scheduleGroupId = currentScheduleGroupId ?: FocusTimerService.currentScheduleGroupId.value ?: "unknown"
                val scheduleType = currentScheduleType
                val todoResults = TodoResultBuilder.buildTodoResult(scheduleGroupId, scheduleInfo, responses)
                
                if (todoResults != null && sessionId != null) {
                    val resultEntity = FocusSessionTodoResultEntity(
                        sessionId = sessionId,
                        scheduleId = scheduleGroupId,
                        scheduleType = scheduleType,
                        scheduleTitleSnapshot = TodoResultBuilder.getScheduleTitleSnapshot(scheduleInfo, ""),
                        todoResultsJson = com.allday.detoxy.data.local.converter.TodoResultConverter.toJson(todoResults),
                        completedAt = System.currentTimeMillis()
                    )
                    todoResultDao.insert(resultEntity)
                    Log.i(TAG, "✅ Todo results saved: ${todoResults.size} items")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save todo results: ${e.message}", e)
            } finally {
                currentSessionId = null
                _currentScheduleInfo.value = null
                currentScheduleGroupId = null  // 리뷰 피드백: 메타데이터도 초기화
            }
        }
    }
    
    /**
     * 🆕 v9: 목표 달성 다이얼로그 완료 콜백
     */
    fun onGoalDialogComplete(completed: Boolean) {
        _showGoalDialog.value = false
        
        val scheduleInfo = _currentScheduleInfo.value ?: return
        val sessionId = currentSessionId
        
        // 리뷰 피드백: 세션 시작 시 저장한 값 사용
        val scheduleGroupId = currentScheduleGroupId ?: FocusTimerService.currentScheduleGroupId.value ?: "unknown"
        val scheduleType = currentScheduleType
        
        val goalStatus = if (completed) TodoCompletionStatus.COMPLETED 
                         else TodoCompletionStatus.NOT_COMPLETED
        
        // goalId에 대한 응답으로 변환
        val responses = mapOf("goal:$scheduleGroupId" to goalStatus)
        
        viewModelScope.launch {
            try {
                val todoResults = TodoResultBuilder.buildTodoResult(scheduleGroupId, scheduleInfo, responses)
                
                if (todoResults != null && sessionId != null) {
                    val resultEntity = FocusSessionTodoResultEntity(
                        sessionId = sessionId,
                        scheduleId = scheduleGroupId,
                        scheduleType = scheduleType,
                        scheduleTitleSnapshot = scheduleInfo.title,
                        todoResultsJson = com.allday.detoxy.data.local.converter.TodoResultConverter.toJson(todoResults),
                        completedAt = System.currentTimeMillis()
                    )
                    todoResultDao.insert(resultEntity)
                    Log.i(TAG, "✅ Goal result saved: completed=$completed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save goal result: ${e.message}", e)
            } finally {
                currentSessionId = null
                _currentScheduleInfo.value = null
                currentScheduleGroupId = null
            }
        }
    }
    
    /**
     * 🆕 v9: 다이얼로그 닫기 (무응답 처리)
     */
    fun onDialogDismiss() {
        val scheduleInfo = _currentScheduleInfo.value
        
        if (_showTodoDialog.value) {
            // 할일 다이얼로그 무응답 처리
            scheduleInfo?.let {
                val responses = TodoResultBuilder.handleNoResponse(it.todos)
                onTodoDialogComplete(responses)
            }
            _showTodoDialog.value = false
        } else if (_showGoalDialog.value) {
            // 목표 다이얼로그 무응답 처리 (미완료로 기록)
            onGoalDialogComplete(false)
            _showGoalDialog.value = false
        }
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
        
        // ViewModel 종료 시 타이머도 중지 -> 🔥 제거: 백그라운드 실행 유지를 위해 주석 처리
        // if (timerState.value == FocusState.RUNNING) {
        //     resetTimer()
        // }
        
        Log.d(TAG, "✅ TimerViewModel cleared")
    }

    /**
     * 다음 예약 정보 계산
     *
     * 활성화된 예약 중 현재 시각 이후 가장 가까운 예약을 찾아 UI용 텍스트를 생성합니다.
     *
     * @param autoRuns 활성화된 예약 리스트
     * @return UI 표시용 텍스트 (예: "다음 예약: 오늘 23:17 (오후 업무)") 또는 null
     */
    private fun calculateNextAutoRunInfo(autoRuns: List<TimeBasedAutoRun>): String? {
        if (autoRuns.isEmpty()) return null

        val now = Calendar.getInstance()
        @Suppress("UNUSED_VARIABLE")
        val currentDay = getDayOfWeekCode(now)
        @Suppress("UNUSED_VARIABLE")
        val currentTimeMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        var nearestAutoRun: TimeBasedAutoRun? = null
        var nearestTimeMillis = Long.MAX_VALUE

        // 모든 활성화된 예약에 대해 다음 트리거 시각 계산
        for (autoRun in autoRuns) {
            val enabledDays = parseEnabledDays(autoRun.enabledDays)
            if (enabledDays.isEmpty()) continue

            // 오늘부터 14일 내에서 다음 발생 시각 찾기
            for (dayOffset in 0..13) {
                val targetCalendar = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, dayOffset)
                }
                val targetDay = getDayOfWeekCode(targetCalendar)

                // 해당 요일이 활성화되어 있는지 확인
                if (!enabledDays.contains(targetDay)) continue

                // 시각 설정
                targetCalendar.set(Calendar.HOUR_OF_DAY, autoRun.hour)
                targetCalendar.set(Calendar.MINUTE, autoRun.minute)
                targetCalendar.set(Calendar.SECOND, 0)
                targetCalendar.set(Calendar.MILLISECOND, 0)

                val targetTimeMillis = targetCalendar.timeInMillis

                // 미래 시각이고, 현재까지 찾은 가장 가까운 시각보다 가까우면 업데이트
                if (targetTimeMillis > now.timeInMillis && targetTimeMillis < nearestTimeMillis) {
                    nearestTimeMillis = targetTimeMillis
                    nearestAutoRun = autoRun
                    // 이 autoRun에서 가장 가까운 예약을 찾았으므로 다음 날짜는 확인하지 않음
                    break
                }
            }
            // 모든 autoRun을 확인하여 전체 중 가장 가까운 예약을 찾아야 하므로 여기서 break하지 않음
        }

        // 가장 가까운 예약이 없으면 null 반환
        if (nearestAutoRun == null) return null

        // UI 표시용 텍스트 생성
        return formatNextAutoRunText(nearestTimeMillis, nearestAutoRun)
    }

    /**
     * 다음 예약 텍스트 포맷팅
     *
     * @param timeMillis 예약 시각 (epoch millis)
     * @param autoRun 예약 정보
     * @return 포맷된 텍스트 (예: "다음 예약: 오늘 23:17 (오후 업무)")
     */
    private fun formatNextAutoRunText(timeMillis: Long, autoRun: TimeBasedAutoRun): String {
        val targetCalendar = Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
        }
        val now = Calendar.getInstance()

        // 날짜 표현 ("오늘", "내일", "월요일" 등)
        val dayText = when {
            isSameDay(now, targetCalendar) -> "오늘"
            isTomorrow(now, targetCalendar) -> "내일"
            else -> getDayOfWeekName(targetCalendar)
        }

        // 시각 포맷 (HH:mm)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeText = timeFormat.format(targetCalendar.time)

        // 라벨 표시 (있으면)
        val labelText = if (!autoRun.label.isNullOrBlank()) {
            " (${autoRun.label})"
        } else {
            ""
        }

        return "다음 예약: $dayText $timeText$labelText"
    }

    /**
     * enabledDays JSON 파싱
     *
     * @param enabledDaysJson JSON 문자열 (예: "[\"MON\",\"WED\",\"FRI\"]")
     * @return 요일 코드 Set (예: ["MON", "WED", "FRI"])
     */
    private fun parseEnabledDays(enabledDaysJson: String): Set<String> {
        return try {
            enabledDaysJson
                .removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Calendar에서 요일 코드 가져오기
     *
     * @param calendar Calendar 인스턴스
     * @return 요일 코드 (예: "MON", "TUE", ...)
     */
    private fun getDayOfWeekCode(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MON"
            Calendar.TUESDAY -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY -> "THU"
            Calendar.FRIDAY -> "FRI"
            Calendar.SATURDAY -> "SAT"
            Calendar.SUNDAY -> "SUN"
            else -> "MON"
        }
    }

    /**
     * 요일 한글 이름 가져오기
     *
     * @param calendar Calendar 인스턴스
     * @return 요일 이름 (예: "월요일", "화요일", ...)
     */
    private fun getDayOfWeekName(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "월요일"
            Calendar.TUESDAY -> "화요일"
            Calendar.WEDNESDAY -> "수요일"
            Calendar.THURSDAY -> "목요일"
            Calendar.FRIDAY -> "금요일"
            Calendar.SATURDAY -> "토요일"
            Calendar.SUNDAY -> "일요일"
            else -> ""
        }
    }

    /**
     * 같은 날인지 확인
     */
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * 내일인지 확인
     */
    private fun isTomorrow(now: Calendar, target: Calendar): Boolean {
        val tomorrow = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_MONTH, 1)
        }
        return isSameDay(tomorrow, target)
    }

    /**
     * 커스텀 프리셋 저장
     *
     * @param name 프리셋 이름
     * @param durationMinutes 타이머 시간 (분)
     * @param presetType 차단 프리셋 (옵션)
     */
    fun saveCustomPreset(name: String, durationMinutes: Int, presetType: String?) {
        viewModelScope.launch {
            presetRepository.insert(
                com.allday.detoxy.data.local.entity.CustomTimerPreset(
                    name = name,
                    durationMinutes = durationMinutes,
                    presetType = presetType,
                    usageCount = 0,
                    displayOrder = presetRepository.getCount()
                )
            )
        }
    }

    /**
     * 커스텀 프리셋 업데이트
     *
     * @param preset 업데이트할 프리셋
     */
    fun updateCustomPreset(preset: com.allday.detoxy.data.local.entity.CustomTimerPreset) {
        viewModelScope.launch {
            presetRepository.update(preset)
        }
    }

    /**
     * 커스텀 프리셋 삭제
     *
     * @param presetId 삭제할 프리셋 ID
     */
    fun deleteCustomPreset(presetId: String) {
        viewModelScope.launch {
            presetRepository.delete(presetId)
        }
    }

    /**
     * 프리셋 사용 시 사용 횟수 증가
     *
     * @param presetId 프리셋 ID (null이면 기본 프리셋)
     */
    fun incrementPresetUsage(presetId: String?) {
        if (presetId != null) {
            viewModelScope.launch {
                presetRepository.incrementUsageCount(presetId)
            }
        }
    }
    /**
     * 타이머 스타일 변경 처리
     *
     * @param index 변경된 스타일 인덱스
     */
    fun onTimerStyleChanged(index: Int) {
        if (_selectedTimerStyleIndex.value != index) {
            _selectedTimerStyleIndex.value = index
            viewModelScope.launch {
                val preferenceManager = com.allday.detoxy.core.utils.PreferenceManager(application)
                preferenceManager.setLastTimerStyleIndex(index)
                Log.d(TAG, "💾 Saved timer style index: $index")
            }
        }
    }
}
