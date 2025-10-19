package com.allday.detoxy.service.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import com.allday.detoxy.MainActivity
import com.allday.detoxy.R
import com.allday.detoxy.service.accessibility.FocusAccessibilityService
import dagger.hilt.android.AndroidEntryPoint

/**
 * 오버레이 잠금 화면 서비스
 *
 * 타이머 실행 중 차단 앱 실행 시 전체 화면 오버레이를 표시합니다.
 * 포그라운드 서비스로 실행되어 시스템에 의한 종료를 방지합니다.
 * XML 레이아웃과 TextView를 사용하여 안정적인 UI 업데이트 제공
 */
@AndroidEntryPoint
class LockOverlayService : LifecycleService() {

    companion object {
        private const val TAG = "LockOverlayService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "lock_overlay_channel"
        private const val CHANNEL_NAME = "잠금 화면"

        const val ACTION_SHOW_OVERLAY = "com.allday.detoxy.ACTION_SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.allday.detoxy.ACTION_HIDE_OVERLAY"
        const val ACTION_GIVE_UP = "com.allday.detoxy.ACTION_GIVE_UP"

        const val EXTRA_REMAINING_SECONDS = "remaining_seconds"
        const val EXTRA_TOTAL_SECONDS = "total_seconds"

        /**
         * 오버레이 표시
         */
        fun showOverlay(context: Context, remainingSeconds: Int, totalSeconds: Int) {
            Log.d(TAG, "📞 showOverlay() called - remainingSeconds: $remainingSeconds, totalSeconds: $totalSeconds")

            // 권한 확인
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val hasPermission = android.provider.Settings.canDrawOverlays(context)
                Log.d(TAG, "🔐 Overlay permission check: ${if (hasPermission) "✅ GRANTED" else "❌ DENIED"}")
                if (!hasPermission) {
                    Log.e(TAG, "Cannot show overlay - permission not granted!")
                }
            }

            val intent = Intent(context, LockOverlayService::class.java).apply {
                action = ACTION_SHOW_OVERLAY
                putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
                putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
            }

            Log.d(TAG, "🚀 Starting LockOverlayService with ACTION_SHOW_OVERLAY")
            context.startService(intent)
        }

        /**
         * 오버레이 숨김
         */
        fun hideOverlay(context: Context) {
            val intent = Intent(context, LockOverlayService::class.java).apply {
                action = ACTION_HIDE_OVERLAY
            }
            context.startService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayShowing = false
    private var timerJob: Job? = null

    // UI 요소
    private var timerTextView: TextView? = null
    private var progressBar: ProgressBar? = null

    // 타이머 상태
    private var currentRemainingSeconds = 0
    private var currentTotalSeconds = 0

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()

        Log.d(TAG, "LockOverlayService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🔔 onStartCommand() - action: ${intent?.action}")

        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> {
                val remainingSeconds = intent.getIntExtra(EXTRA_REMAINING_SECONDS, 0)
                val totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, 0)
                Log.d(TAG, "📍 ACTION_SHOW_OVERLAY received - remainingSeconds: $remainingSeconds, totalSeconds: $totalSeconds")
                showOverlay(remainingSeconds, totalSeconds)
            }
            ACTION_HIDE_OVERLAY -> {
                Log.d(TAG, "📍 ACTION_HIDE_OVERLAY received")
                hideOverlay()
                stopSelf()
            }
            ACTION_GIVE_UP -> {
                Log.d(TAG, "📍 ACTION_GIVE_UP received")
                handleGiveUp()
            }
            else -> {
                Log.w(TAG, "⚠️ Unknown action: ${intent?.action}")
            }
        }

        // 포그라운드 서비스 시작
        startForeground(NOTIFICATION_ID, createNotification())

        // super 호출 (lint MissingSuperCall 해결)
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    /**
     * 오버레이 화면 표시
     */
    private fun showOverlay(remainingSeconds: Int, totalSeconds: Int) {
        // 오버레이 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            Log.e(TAG, "❌ Overlay permission not granted! Cannot show overlay.")
            Log.e(TAG, "Please grant 'Display over other apps' permission in settings")
            return
        }

        // 기존 오버레이가 있으면 제거 (새로운 시간으로 업데이트하기 위함)
        if (isOverlayShowing) {
            Log.d(TAG, "Removing existing overlay to update with new time: $remainingSeconds seconds")
            hideOverlay()
        }

        try {
            Log.d(TAG, "✅ Overlay permission granted")
            Log.d(TAG, "Creating new overlay - Remaining: $remainingSeconds, Total: $totalSeconds")

            // 초기값 설정
            currentRemainingSeconds = remainingSeconds
            currentTotalSeconds = totalSeconds

            // XML 레이아웃 inflate
            Log.d(TAG, "📋 Inflating XML layout...")
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            overlayView = inflater.inflate(R.layout.lock_overlay_layout, null)

            // View 참조 획득
            timerTextView = overlayView?.findViewById(R.id.timerText)
            progressBar = overlayView?.findViewById(R.id.progressBar)
            val closeButton = overlayView?.findViewById<Button>(R.id.closeButton)

            // 초기 UI 업데이트
            updateTimerDisplay()

            // 닫기 버튼 클릭 리스너 (오버레이 숨김 + 홈 화면 이동, 타이머는 계속 실행)
            closeButton?.setOnClickListener {
                Log.d(TAG, "❌ Close button clicked - hiding overlay and navigating to home (timer continues)")
                hideOverlay(stopTimer = false)
                
                // 홈 화면으로 이동하여 차단된 앱이 다시 포그라운드로 오지 않도록 보장
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                
                try {
                    startActivity(homeIntent)
                    Log.d(TAG, "✅ Navigated to home screen after closing overlay")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to navigate to home: ${e.message}", e)
                }
            }

            Log.d(TAG, "✅ Layout inflated and views initialized")

            // WindowManager 파라미터 설정
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                },
                // FLAG_NOT_FOCUSABLE 제거: 오버레이가 포커스를 받아 최상위에 표시됨
                // FLAG_NOT_TOUCH_MODAL 제거: 오버레이 밖의 터치를 차단함
                // FLAG_FULLSCREEN: 전체 화면 모드로 표시
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            // 오버레이 추가
            Log.d(TAG, "🪟 WindowManager adding view...")
            Log.d(TAG, "   - WindowManager: ${windowManager != null}")
            Log.d(TAG, "   - OverlayView: ${overlayView != null}")
            Log.d(TAG, "   - Params: TYPE=${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) "TYPE_APPLICATION_OVERLAY" else "TYPE_SYSTEM_ALERT"}")

            windowManager?.addView(overlayView, params)
            isOverlayShowing = true
            Log.d(TAG, "✅ Overlay shown successfully: $currentRemainingSeconds / $currentTotalSeconds seconds")

            // 타이머 시작
            startTimerUpdate()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay: ${e.message}", e)
        }
    }

    /**
     * 타이머 업데이트 시작
     * TextView를 직접 업데이트하여 UI 갱신
     */
    private fun startTimerUpdate() {
        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            while (isActive && currentRemainingSeconds > 0) {
                delay(1000)
                // Main 스레드에서 TextView 직접 업데이트
                withContext(Dispatchers.Main) {
                    currentRemainingSeconds--
                    updateTimerDisplay()

                    if (currentRemainingSeconds % 5 == 0 || currentRemainingSeconds <= 5) {
                        Log.d(TAG, "🕒 Timer update: $currentRemainingSeconds seconds remaining")
                    }
                }
            }

            if (currentRemainingSeconds == 0) {
                Log.d(TAG, "✅ Timer finished")
                hideOverlay()
                stopSelf()
            }
        }
    }

    /**
     * 타이머 UI 업데이트
     */
    private fun updateTimerDisplay() {
        val minutes = currentRemainingSeconds / 60
        val seconds = currentRemainingSeconds % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)
        
        timerTextView?.text = timeText

        // 진행률 계산 (0-100)
        if (currentTotalSeconds > 0) {
            val elapsed = currentTotalSeconds - currentRemainingSeconds
            val progress = (elapsed.toFloat() / currentTotalSeconds.toFloat() * 100).toInt()
            progressBar?.progress = progress
        }
    }

    /**
     * 오버레이 화면 숨김
     * @param stopTimer true면 타이머도 중지, false면 오버레이만 숨김
     */
    private fun hideOverlay(stopTimer: Boolean = true) {
        // 타이머 중지 (stopTimer가 true인 경우만)
        if (stopTimer) {
            timerJob?.cancel()
            timerJob = null
            Log.d(TAG, "Timer stopped")
        } else {
            Log.d(TAG, "Timer continues in background")
        }

        if (!isOverlayShowing || overlayView == null) {
            return
        }

        try {
            windowManager?.removeView(overlayView)
            overlayView = null
            timerTextView = null
            progressBar = null
            isOverlayShowing = false
            Log.d(TAG, "Overlay hidden (stopTimer: $stopTimer)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide overlay: ${e.message}", e)
        }
    }

    /**
     * 포기 버튼 처리
     */
    private fun handleGiveUp() {
        Log.d(TAG, "User gave up timer")

        // AccessibilityService 상태 초기화
        FocusAccessibilityService.isTimerRunning = false
        FocusAccessibilityService.remainingSeconds = 0
        FocusAccessibilityService.totalSeconds = 0

        // 오버레이 숨기기
        hideOverlay()

        // MainActivity를 시작하여 타이머 선택 화면으로 이동
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("TIMER_GAVE_UP", true)  // 타이머 포기 상태 전달
        }
        startActivity(intent)

        // 서비스 종료
        stopSelf()
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
                description = "잠금 화면 서비스 실행 중"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * 포그라운드 서비스 알림 생성
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

        return builder
            .setContentTitle("집중 모드 실행 중")
            .setContentText("차단 앱 사용 시 잠금 화면이 표시됩니다")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🔴 onDestroy() - Cleaning up resources")
        
        // 타이머 Job 명시적 취소
        timerJob?.cancel()
        timerJob = null
        
        // 오버레이 뷰 제거
        hideOverlay()
        
        // WindowManager 참조 해제
        windowManager = null
        
        Log.d(TAG, "✅ LockOverlayService destroyed - All resources cleaned up")
    }
}
