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
import android.util.Log
import com.allday.detoxy.MainActivity
import com.allday.detoxy.R
import com.allday.detoxy.domain.model.FocusState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 집중 모드 타이머 Foreground Service
 * 
 * 화면 상태(슬립 모드, 백그라운드)와 무관하게 타이머를 실행합니다.
 * Foreground Service로 실행되어 시스템에 의한 종료를 방지하고,
 * StateFlow를 통해 UI에 실시간 상태를 전달합니다.
 */
class FocusTimerService : Service() {

    companion object {
        private const val TAG = "FocusTimerService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "focus_timer_channel"
        private const val CHANNEL_NAME = "집중 모드 타이머"

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

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        createNotificationChannel()
        Log.d(TAG, "FocusTimerService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand() - action: ${intent?.action}")

        when (intent?.action) {
            ACTION_START, ACTION_START_TIMER -> { // 🔄 ACTION_START 지원 추가
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 0)
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                val autoRunId = intent.getStringExtra(EXTRA_AUTO_RUN_ID) // 🆕
                val autoRunLabel = intent.getStringExtra(EXTRA_AUTO_RUN_LABEL) // 🆕
                Log.d(TAG, "Starting timer: $durationMinutes minutes, sessionId: $sessionId, autoRunId: $autoRunId")
                startTimerInternal(durationMinutes, sessionId)
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

        // Foreground Service 시작
        startForeground(NOTIFICATION_ID, createNotification())

        return START_STICKY // 시스템에 의해 종료되어도 재시작
    }

    /**
     * 타이머 내부 시작 로직
     */
    private fun startTimerInternal(durationMinutes: Int, sessionId: String?) {
        // 이미 실행 중이면 무시
        if (_state.value == FocusState.RUNNING) {
            Log.w(TAG, "Timer already running, ignoring start request")
            return
        }

        val totalSec = durationMinutes * 60
        _totalSeconds.value = totalSec
        _remainingSeconds.value = totalSec
        _state.value = FocusState.RUNNING
        _currentSessionId.value = sessionId

        Log.d(TAG, "Timer started: $totalSec seconds")

        // 타이머 Job 시작 (Dispatchers.Default에서 실행하여 메인 스레드 부하 방지)
        timerJob?.cancel()
        timerJob = serviceScope?.launch {
            try {
                while (_remainingSeconds.value > 0 && _state.value == FocusState.RUNNING) {
                    delay(1000) // 1초 대기
                    
                    // StateFlow 업데이트
                    val newRemaining = _remainingSeconds.value - 1
                    _remainingSeconds.value = newRemaining

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

        // 타이머 완료/포기 브로드캐스트
        if (previousState == FocusState.RUNNING) {
            sendTimerFinishedBroadcast(success)
        }

        // 상태 초기화
        _remainingSeconds.value = 0
        _currentSessionId.value = null

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
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "집중 모드 타이머 실행 중"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
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
        
        // 타이머 Job 취소
        timerJob?.cancel()
        timerJob = null
        
        // 코루틴 스코프 취소
        serviceScope?.cancel()
        serviceScope = null
        
        // 상태 초기화
        _state.value = FocusState.IDLE
        _remainingSeconds.value = 0
        _totalSeconds.value = 0
        _currentSessionId.value = null
    }
}

