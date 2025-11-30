package com.allday.detoxy.service.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.allday.detoxy.MainActivity
import com.allday.detoxy.R
import com.allday.detoxy.core.manager.DndManager
import com.allday.detoxy.core.utils.AppCategoryMapper
import com.allday.detoxy.domain.model.FocusState
import com.allday.detoxy.domain.repository.FocusSettingsRepository
import com.allday.detoxy.service.accessibility.FocusAccessibilityService
import com.allday.detoxy.service.overlay.LockOverlayService // 🆕 추가
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 집중 모드 타이머 Foreground Service
 * 
 * 화면 상태(슬립 모드, 백그라운드)와 무관하게 타이머를 실행합니다.
 * Foreground Service로 실행되어 시스템에 의한 종료를 방지하고,
 * StateFlow를 통해 UI에 실시간 상태를 전달합니다.
 */
@AndroidEntryPoint
class FocusTimerService : Service() {

    @Inject
    lateinit var settingsRepository: FocusSettingsRepository

    @Inject
    lateinit var timeBasedAutoRunDao: com.allday.detoxy.data.local.dao.TimeBasedAutoRunDao  // 🆕 v0.10.1

    @Inject
    lateinit var autoRunLogDao: com.allday.detoxy.data.local.dao.AutoRunLogDao  // 🆕 위치 기반 AutoRunLog 업데이트용

    @Inject
    lateinit var locationBasedAutoRunDao: com.allday.detoxy.data.local.dao.LocationBasedAutoRunDao  // 🆕 위치 기반 AutoRunLog 업데이트용

    companion object {
        private const val TAG = "FocusTimerService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "focus_timer_channel"
        private const val CHANNEL_NAME = "집중 모드 타이머"
        
        // 🆕 성공 알림 전용 채널 (높은 중요도)
        private const val SUCCESS_CHANNEL_ID = "focus_success_channel"
        private const val SUCCESS_CHANNEL_NAME = "집중 성공 알림"

        const val ACTION_START = "com.allday.detoxy.ACTION_START" // 🆕 간소화된 ACTION
        const val ACTION_START_TIMER = "com.allday.detoxy.ACTION_START_TIMER"
        const val ACTION_STOP_TIMER = "com.allday.detoxy.ACTION_STOP_TIMER"
        const val ACTION_GIVE_UP_TIMER = "com.allday.detoxy.ACTION_GIVE_UP_TIMER"
        const val ACTION_TIMER_FINISHED = "com.allday.detoxy.TIMER_FINISHED"

        const val EXTRA_DURATION_MINUTES = "duration_minutes"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_SUCCESS = "success"
        const val EXTRA_PRESET_TYPE = "preset_type" // 🆕 차단 프리셋 타입
        const val EXTRA_AUTO_RUN_ID = "auto_run_id" // 🆕 자동 실행 ID
        const val EXTRA_AUTO_RUN_LABEL = "auto_run_label" // 🆕 자동 실행 라벨

        // Service 상태를 Static으로 관리하여 어디서든 접근 가능
        private val _state = MutableStateFlow(FocusState.IDLE)
        val state: StateFlow<FocusState> = _state.asStateFlow()

        private val _remainingSeconds = MutableStateFlow(0)
        val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

        private val _totalSeconds = MutableStateFlow(0)
        val totalSeconds: StateFlow<Int> = _totalSeconds.asStateFlow()

        private val _currentSessionId = MutableStateFlow<String?>(null)
        val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

        // 🆕 현재 작동 중인 AutoRun ID (v0.10.1 - 작동/활성 구분)
        private val _currentAutoRunId = MutableStateFlow<String?>(null)
        val currentAutoRunId: StateFlow<String?> = _currentAutoRunId.asStateFlow()

        // 🆕 현재 작동 중인 ScheduleGroup ID (v0.10.1 - 작동/활성 구분)
        private val _currentScheduleGroupId = MutableStateFlow<String?>(null)
        val currentScheduleGroupId: StateFlow<String?> = _currentScheduleGroupId.asStateFlow()

        /**
         * 타이머 시작
         */
        fun startTimer(context: Context, durationMinutes: Int, sessionId: String) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_START_TIMER
                putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            context.startService(intent)
        }

        /**
         * 타이머 중지 (정상 완료)
         */
        fun stopTimer(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_STOP_TIMER
            }
            context.startService(intent)
        }

        /**
         * 타이머 포기
         */
        fun giveUpTimer(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_GIVE_UP_TIMER
            }
            context.startService(intent)
        }
    }

    private var serviceScope: CoroutineScope? = null
    private var timerJob: Job? = null
    private lateinit var dndManager: DndManager
    private var wakeLock: PowerManager.WakeLock? = null  // 🔥 v0.10.1.3: WakeLock
    
    // 🆕 onCreate()에서 미리 생성한 간단한 알림 (startForeground() 즉시 호출용)
    private var preCreatedNotification: Notification? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🔵 onCreate() called - Service instance created")
        serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        dndManager = DndManager(applicationContext)
        
        // 🔥 v0.10.1.3: WakeLock 초기화 (CPU를 깨어있게 유지)
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AllDayDetoxy::FocusTimerWakeLock"
        ).apply {
            setReferenceCounted(false) // 여러 번 acquire/release 해도 한 번만 해제되도록
        }
        
        createNotificationChannel()
        
        // 🆕 onCreate()에서 미리 간단한 알림 생성 (onStartCommand에서 즉시 사용)
        preCreatedNotification = createSimpleNotification()
        
        Log.d(TAG, "✅ onCreate() completed - WakeLock initialized, notification pre-created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🟢 onStartCommand() ENTERED - action: ${intent?.action}, startId: $startId")
        
        // ⚠️ CRITICAL: startForeground()를 첫 줄에서 즉시 호출하여 서비스가 종료되지 않도록 보호
        // Android 시스템은 startForeground() 호출 전에 서비스를 종료할 수 있습니다.
        // onCreate()에서 미리 생성한 알림을 사용하여 최대한 빠르게 호출합니다.
        val notification = preCreatedNotification ?: run {
            Log.w(TAG, "⚠️ preCreatedNotification is null, creating simple notification on the fly")
            createSimpleNotification()
        }
        
        Log.d(TAG, "📢 About to call startForeground()...")
        
        // 🆕 Android 14+ (API 34+): 서비스 타입 지정 필수
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("NewApi")
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        Log.d(TAG, "✅ startForeground() called successfully - action: ${intent?.action}")
        
        // 상세 알림으로 즉시 업데이트 (비동기로 처리하여 블로킹 방지)
        serviceScope?.launch {
            updateNotification()
        } ?: run {
            // serviceScope가 null이면 동기적으로 업데이트
            updateNotification()
        }

        when (intent?.action) {
            ACTION_START, ACTION_START_TIMER -> { // 🔄 ACTION_START 지원 추가
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 0)
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                val presetType = intent.getStringExtra(EXTRA_PRESET_TYPE) // 🔥 프리셋 타입
                val autoRunId = intent.getStringExtra(EXTRA_AUTO_RUN_ID) // 🆕
                val autoRunLabel = intent.getStringExtra(EXTRA_AUTO_RUN_LABEL) // 🆕
                Log.d(TAG, "Starting timer: $durationMinutes minutes, sessionId: $sessionId, presetType: $presetType, autoRunId: $autoRunId")
                
                // 🆕 v0.10.1: autoRunId로부터 scheduleGroupId 조회
                val scope = serviceScope
                if (scope == null) {
                    Log.e(TAG, "❌ serviceScope is null, cannot start timer!")
                    // serviceScope가 null이면 직접 시작 (fallback)
                    startTimerInternal(durationMinutes, sessionId, presetType, autoRunId, null)
                } else {
                    scope.launch {
                        Log.d(TAG, "🔍 Looking up scheduleGroupId for autoRunId: $autoRunId")
                        val scheduleGroupId = if (autoRunId != null) {
                            try {
                                val autoRun = withContext(Dispatchers.IO) {
                                    timeBasedAutoRunDao.getById(autoRunId).first()
                                }
                                Log.d(TAG, "✅ Found autoRun: ${autoRun?.id}, scheduleGroupId: ${autoRun?.scheduleGroupId}")
                                autoRun?.scheduleGroupId
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Failed to get scheduleGroupId from autoRunId: ${e.message}", e)
                                null
                            }
                        } else {
                            Log.d(TAG, "ℹ️ autoRunId is null, skipping scheduleGroupId lookup")
                            null
                        }
                        
                        // 메인 스레드에서 startTimerInternal 호출
                        Log.d(TAG, "🚀 Calling startTimerInternal with scheduleGroupId: $scheduleGroupId")
                        withContext(Dispatchers.Main) {
                            startTimerInternal(durationMinutes, sessionId, presetType, autoRunId, scheduleGroupId)
                        }
                    }
                }
            }
            ACTION_STOP_TIMER -> {
                Log.d(TAG, "Stopping timer")
                stopTimerInternal(success = true)
            }
            ACTION_GIVE_UP_TIMER -> {
                Log.d(TAG, "Giving up timer")
                stopTimerInternal(success = false)
            }
        }

        return START_STICKY // 시스템에 의해 종료되어도 재시작
    }

    /**
     * 타이머 내부 시작 로직
     * 
     * @param durationMinutes 타이머 기간 (분)
     * @param sessionId 세션 ID
     * @param presetType 차단 프리셋 타입 (v0.10.1.1)
     * @param autoRunId 자동 실행 ID (v0.10.1)
     * @param scheduleGroupId 스케줄 그룹 ID (v0.10.1)
     */
    private fun startTimerInternal(
        durationMinutes: Int,
        sessionId: String?,
        presetType: String? = null,       // 🔥 v0.10.1.1: 차단 프리셋
        autoRunId: String? = null,        // 🆕 v0.10.1
        scheduleGroupId: String? = null   // 🆕 v0.10.1
    ) {
        Log.d(TAG, "🎯 startTimerInternal called: duration=$durationMinutes, sessionId=$sessionId, autoRunId=$autoRunId, scheduleGroupId=$scheduleGroupId")
        
        // 이미 실행 중이면 무시
        if (_state.value == FocusState.RUNNING) {
            Log.w(TAG, "⚠️ Timer already running (state: ${_state.value}), ignoring start request")
            return
        }

        val totalSec = durationMinutes * 60
        _totalSeconds.value = totalSec
        _remainingSeconds.value = totalSec
        _state.value = FocusState.RUNNING
        _currentSessionId.value = sessionId
        
        // 🆕 v0.10.1: 작동 중인 AutoRun/ScheduleGroup 정보 저장
        _currentAutoRunId.value = autoRunId
        _currentScheduleGroupId.value = scheduleGroupId
        
        Log.d(TAG, "Timer started: $totalSec seconds, autoRunId=$autoRunId, scheduleGroupId=$scheduleGroupId")

        // 🆕 위치 기반 자동 실행 AutoRunLog sessionId 업데이트
        // scheduleGroupId가 있는 경우 (위치 기반으로 활성화된 시간표일 수 있음)
        // 해당 scheduleGroupId에 연결된 위치의 최근 AutoRunLog를 찾아서 sessionId 업데이트
        // 최근 10분 내의 LOCATION 타입 STARTED 로그를 찾아 연결
        if (sessionId != null && scheduleGroupId != null) {
            serviceScope?.launch {
                try {
                    updateLocationBasedAutoRunLog(sessionId, scheduleGroupId)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to update location-based AutoRunLog: ${e.message}", e)
                }
            }
        }

        // 🔥 v0.10.1.3: WakeLock 획득 (CPU를 깨어있게 유지하여 타이머 정확성 보장)
        try {
            wakeLock?.acquire(totalSec * 1000L + 10000L) // 타이머 시간 + 10초 여유
            Log.i(TAG, "✅ WakeLock acquired (${totalSec}s + 10s)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to acquire WakeLock: ${e.message}", e)
        }

        // ⭐ Critical: AccessibilityService 설정 먼저 동기적으로 활성화
        // 타이머 정보 전달을 먼저 하여 앱 차단이 즉시 동작하도록 함
        FocusAccessibilityService.isTimerRunning = true
        FocusAccessibilityService.remainingSeconds = totalSec
        FocusAccessibilityService.totalSeconds = totalSec
        FocusAccessibilityService.currentSessionId = sessionId
        Log.i(TAG, "✅ [1/2] AccessibilityService activated (isTimerRunning=true, sessionId=$sessionId)")

        // 🔥 v0.10.1.1: 디톡시 제어 설정 로드 및 AccessibilityService에 전달
        serviceScope?.launch {
            try {
                if (presetType != null) {
                    // presetType이 지정된 경우 (자동 실행 시) 해당 프리셋 적용
                    Log.i(TAG, "🎯 Applying preset from AutoRun: $presetType")
                    val preset = when (presetType) {
                        "STANDARD" -> AppCategoryMapper.DetoxyPreset.STANDARD_DETOXY
                        "RELAXED" -> AppCategoryMapper.DetoxyPreset.RELAXED
                        "FULL_BLOCK" -> AppCategoryMapper.DetoxyPreset.COMPLETE_BLOCK
                        "CUSTOM" -> {
                            // CUSTOM인 경우 저장된 설정 사용
                            val (categories, otherApps) = settingsRepository.getCurrentSettings()
                            FocusAccessibilityService.updateBlockSettings(categories, otherApps)
                            Log.i(TAG, "✅ [2/2] Block settings loaded (CUSTOM): ${categories.joinToString(", ") { it.name }}, otherApps=$otherApps")
                            return@launch
                        }
                        else -> AppCategoryMapper.DetoxyPreset.STANDARD_DETOXY
                    }
                    FocusAccessibilityService.applyPreset(preset)
                    Log.i(TAG, "✅ [2/2] Preset applied: $presetType")
                } else {
                    // presetType이 없는 경우 (수동 실행 시) 저장된 설정 사용
                    val (categories, otherApps) = settingsRepository.getCurrentSettings()
                    FocusAccessibilityService.updateBlockSettings(categories, otherApps)
                    Log.i(TAG, "✅ [2/2] Block settings loaded: ${categories.joinToString(", ") { it.name }}, otherApps=$otherApps")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load block settings: ${e.message}", e)
                // 실패 시 기본 프리셋 사용
                FocusAccessibilityService.applyPreset(AppCategoryMapper.DetoxyPreset.STANDARD_DETOXY)
                Log.w(TAG, "⚠️ Using default preset (STANDARD_DETOXY)")
            }
        }

        // DND 모드 활성화 (Android 6.0 이상, 권한 있을 경우만)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dndManager.enableDnd()
            Log.d(TAG, "✅ DND mode enabled")
        }

        // 타이머 Job 시작 (Dispatchers.Default에서 실행하여 메인 스레드 부하 방지)
        timerJob?.cancel()
        timerJob = serviceScope?.launch {
            try {
                while (_remainingSeconds.value > 0 && _state.value == FocusState.RUNNING) {
                    delay(1000) // 1초 대기
                    
                    // StateFlow 업데이트
                    val newRemaining = _remainingSeconds.value - 1
                    _remainingSeconds.value = newRemaining
                    
                    // AccessibilityService의 remainingSeconds도 동기화
                    FocusAccessibilityService.remainingSeconds = newRemaining

                    // 디버깅: 30초마다 로그 출력
                    if (newRemaining % 30 == 0 || newRemaining <= 5) {
                        Log.d(TAG, "Timer update: $newRemaining seconds remaining")
                    }

                    // 알림 업데이트 (10초마다)
                    if (newRemaining % 10 == 0) {
                        updateNotification()
                    }
                }

                // 정상 완료
                if (_state.value == FocusState.RUNNING && _remainingSeconds.value == 0) {
                    Log.d(TAG, "Timer finished successfully")
                    _state.value = FocusState.FINISHED
                    
                    // 🆕 성공 피드백 수정 (v0.10.2)
                    // 1. 오버레이 애니메이션 제거 (조용히 사라짐)
                    // 2. 성공 상태 저장 (앱 실행 시 축하 애니메이션 표시용)
                    val preferenceManager = com.allday.detoxy.core.utils.PreferenceManager(applicationContext)
                    preferenceManager.setPendingSuccessAnimation(true)
                    
                    // 3. 알림 발송 (시스템 트레이에 남음)
                    showSuccessNotification()
                    
                    // 타이머 완료 브로드캐스트 (ViewModel이 세션 종료 처리)
                    sendTimerFinishedBroadcast(success = true)
                    
                    // 브로드캐스트가 전달될 시간을 주기 위해 지연 후 Service 종료
                    delay(500)
                    stopSelf()
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Timer job cancelled")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Timer error: ${e.message}", e)
            }
        }
    }

    /**
     * 타이머 내부 중지 로직
     */
    private fun stopTimerInternal(success: Boolean) {
        timerJob?.cancel()
        timerJob = null

        val previousState = _state.value
        _state.value = if (success) FocusState.FINISHED else FocusState.FAILED
        
        Log.d(TAG, "Timer stopped: success=$success, previousState=$previousState")

        // 🔥 v0.10.1.3: WakeLock 해제
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.i(TAG, "✅ WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to release WakeLock: ${e.message}", e)
        }

        // AccessibilityService 비활성화
        FocusAccessibilityService.isTimerRunning = false
        FocusAccessibilityService.remainingSeconds = 0
        FocusAccessibilityService.totalSeconds = 0
        FocusAccessibilityService.currentSessionId = null
        Log.i(TAG, "✅ AccessibilityService deactivated (isTimerRunning=false)")

        // DND 모드 비활성화
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dndManager.disableDnd()
            Log.d(TAG, "✅ DND mode disabled")
        }

        // 타이머 완료/포기 브로드캐스트
        if (previousState == FocusState.RUNNING) {
            sendTimerFinishedBroadcast(success)
        }

        // 상태 초기화
        _remainingSeconds.value = 0
        _currentSessionId.value = null
        _currentAutoRunId.value = null         // 🆕 v0.10.1
        _currentScheduleGroupId.value = null   // 🆕 v0.10.1

        // 브로드캐스트가 전달될 시간을 주기 위해 지연 후 Service 종료
        serviceScope?.launch {
            delay(500)
            stopSelf()
        }
    }

    /**
     * 타이머 완료 브로드캐스트 전송
     */
    private fun sendTimerFinishedBroadcast(success: Boolean) {
        val intent = Intent(ACTION_TIMER_FINISHED)
        intent.putExtra(EXTRA_SUCCESS, success)
        intent.putExtra(EXTRA_SESSION_ID, _currentSessionId.value)
        sendBroadcast(intent)
        Log.d(TAG, "Broadcast sent: timer_finished, success=$success")
    }

    /**
     * 알림 채널 생성 (Android 8.0 이상)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // 타이머 실행 중 채널 (낮은 중요도)
            val timerChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "집중 모드 타이머 실행 중"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(timerChannel)
            
            // 🆕 성공 알림 채널 (높은 중요도)
            val successChannel = NotificationChannel(
                SUCCESS_CHANNEL_ID,
                SUCCESS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "집중 모드 성공 알림"
                setShowBadge(true)
                enableVibration(true)
            }
            notificationManager?.createNotificationChannel(successChannel)
        }
    }

    /**
     * 간단한 Foreground Service 알림 생성 (빠른 시작용)
     * 
     * startForeground() 호출을 빠르게 하기 위해 최소한의 작업만 수행합니다.
     * 나중에 createNotification()으로 상세 알림으로 업데이트됩니다.
     */
    private fun createSimpleNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("집중 모드")
            .setContentText("타이머 시작 중...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }
    
    /**
     * Foreground Service 알림 생성
     */
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val minutes = _remainingSeconds.value / 60
        val seconds = _remainingSeconds.value % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        return builder
            .setContentTitle("집중 모드 실행 중")
            .setContentText("남은 시간: $timeText")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // 스와이프로 제거 불가
            .build()
    }

    /**
     * 성공 알림 표시
     */
    private fun showSuccessNotification() {
        Log.d(TAG, "🎉 Showing success notification")
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, SUCCESS_CHANNEL_ID) // 🔥 성공 전용 채널 사용
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = builder
            .setContentTitle("🎉 집중 성공!")
            .setContentText("목표를 달성했습니다. 축하합니다!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(NOTIFICATION_ID + 1, notification) // 기존 알림과 별도 ID 사용
        Log.d(TAG, "✅ Success notification sent (ID: ${NOTIFICATION_ID + 1})")
    }

    /**
     * 알림 업데이트
     */
    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FocusTimerService destroyed")
        
        // 🔥 v0.10.1.3: WakeLock 해제 (안전장치)
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.w(TAG, "⚠️ WakeLock released in onDestroy (unexpected)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to release WakeLock in onDestroy: ${e.message}", e)
        }
        
        // 타이머 Job 취소
        timerJob?.cancel()
        timerJob = null
        
        // 코루틴 스코프 취소
        serviceScope?.cancel()
        serviceScope = null
        
        // AccessibilityService 비활성화 (비정상 종료 대응)
        FocusAccessibilityService.isTimerRunning = false
        FocusAccessibilityService.remainingSeconds = 0
        FocusAccessibilityService.totalSeconds = 0
        FocusAccessibilityService.currentSessionId = null
        Log.i(TAG, "✅ AccessibilityService deactivated on destroy")
        
        // DND 모드 비활성화 (비정상 종료 대응)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dndManager.disableDnd()
            Log.d(TAG, "✅ DND mode disabled on destroy")
        }
        
        // 상태 초기화
        _state.value = FocusState.IDLE
        _remainingSeconds.value = 0
        _totalSeconds.value = 0
        _currentSessionId.value = null
    }

    /**
     * 위치 기반 자동 실행 AutoRunLog의 sessionId 업데이트
     *
     * 위치 기반 이벤트로 시작된 타이머의 경우, Geofence 진입 시점에 기록된 AutoRunLog의
     * sessionId가 null이었으므로, 타이머 시작 시점에 sessionId를 업데이트합니다.
     *
     * @param sessionId 생성된 세션 ID
     * @param scheduleGroupId 활성화된 스케줄 그룹 ID
     */
    private suspend fun updateLocationBasedAutoRunLog(
        sessionId: String,
        scheduleGroupId: String
    ) {
        try {
            Log.d(TAG, "🔍 Updating location-based AutoRunLog: sessionId=$sessionId, scheduleGroupId=$scheduleGroupId")

            // 1. scheduleGroupId에 연결된 위치 기반 자동 실행 목록 조회
            val locations = locationBasedAutoRunDao.getByLinkedGroup(scheduleGroupId)
            
            if (locations.isEmpty()) {
                Log.d(TAG, "ℹ️ No locations linked to scheduleGroupId: $scheduleGroupId")
                return
            }

            val locationIds = locations.map { it.id }
            Log.d(TAG, "📍 Found ${locationIds.size} locations linked to scheduleGroupId: ${locationIds.joinToString(", ")}")

            // 2. 해당 위치들의 최근 STARTED 상태 AutoRunLog 찾기 (최근 10분 내, sessionId가 null인 것만)
            val currentTime = System.currentTimeMillis()
            val recentLog = autoRunLogDao.getRecentLocationStartedLog(locationIds, currentTime)

            if (recentLog == null) {
                Log.w(TAG, "⚠️ No recent location-based AutoRunLog found (LOCATION, STARTED, sessionId=null, within 10 minutes)")
                Log.d(TAG, "   - Searching for locations: ${locationIds.joinToString(", ")}")
                Log.d(TAG, "   - Current time: $currentTime (${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(currentTime))})")
                Log.d(TAG, "   - Search window: ${currentTime - 600000} ~ $currentTime")
                
                // 디버깅: 모든 LOCATION 타입 로그 확인
                try {
                    val allLocationLogs = autoRunLogDao.getLogsInRangeList(currentTime - 600000, currentTime)
                    val locationLogs = allLocationLogs.filter { it.triggerType == "LOCATION" && it.triggerSourceId in locationIds }
                    Log.d(TAG, "   - Found ${locationLogs.size} LOCATION logs in time window:")
                    locationLogs.take(5).forEach { log ->
                        Log.d(TAG, "     * ${log.id}: triggerSourceId=${log.triggerSourceId}, result=${log.result}, sessionId=${log.sessionId}, triggerTime=${log.triggerTime}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "   - Failed to check logs: ${e.message}", e)
                }
                return
            }

            Log.d(TAG, "✅ Found recent AutoRunLog: id=${recentLog.id}, triggerSourceId=${recentLog.triggerSourceId}, triggerTime=${recentLog.triggerTime} (${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(recentLog.triggerTime))})")

            // 3. AutoRunLog의 sessionId 업데이트
            autoRunLogDao.update(
                logId = recentLog.id,
                sessionId = sessionId,
                result = recentLog.result,
                failureReason = recentLog.failureReason
            )

            Log.i(TAG, "✅ Location-based AutoRunLog updated: logId=${recentLog.id}, sessionId=$sessionId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update location-based AutoRunLog: ${e.message}", e)
            throw e
        }
    }
}

