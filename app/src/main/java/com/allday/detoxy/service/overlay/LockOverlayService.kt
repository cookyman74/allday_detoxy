package com.allday.detoxy.service.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import com.allday.detoxy.MainActivity
import com.allday.detoxy.R
import com.allday.detoxy.presentation.ui.overlay.LockOverlayScreen
import com.allday.detoxy.presentation.ui.theme.DetoxyTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 오버레이 잠금 화면 서비스
 *
 * 타이머 실행 중 차단 앱 실행 시 전체 화면 오버레이를 표시합니다.
 * 포그라운드 서비스로 실행되어 시스템에 의한 종료를 방지합니다.
 *
 * @see LockOverlayScreen
 */
@AndroidEntryPoint
class LockOverlayService : Service() {

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
            val intent = Intent(context, LockOverlayService::class.java).apply {
                action = ACTION_SHOW_OVERLAY
                putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
                putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
            }
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
    private var overlayView: ComposeView? = null
    private var isOverlayShowing = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        Log.d(TAG, "LockOverlayService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> {
                val remainingSeconds = intent.getIntExtra(EXTRA_REMAINING_SECONDS, 0)
                val totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, 0)
                showOverlay(remainingSeconds, totalSeconds)
            }
            ACTION_HIDE_OVERLAY -> {
                hideOverlay()
                stopSelf()
            }
            ACTION_GIVE_UP -> {
                handleGiveUp()
            }
        }

        // 포그라운드 서비스 시작
        startForeground(NOTIFICATION_ID, createNotification())

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 오버레이 화면 표시
     */
    private fun showOverlay(remainingSeconds: Int, totalSeconds: Int) {
        if (isOverlayShowing) {
            Log.d(TAG, "Overlay already showing")
            return
        }

        try {
            // ComposeView 생성
            overlayView = ComposeView(this).apply {
                setContent {
                    DetoxyTheme {
                        LockOverlayScreen(
                            remainingSeconds = remainingSeconds,
                            totalSeconds = totalSeconds,
                            onGiveUp = {
                                val intent = Intent(this@LockOverlayService, LockOverlayService::class.java).apply {
                                    action = ACTION_GIVE_UP
                                }
                                startService(intent)
                            }
                        )
                    }
                }
            }

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
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            // 오버레이 추가
            windowManager?.addView(overlayView, params)
            isOverlayShowing = true
            Log.d(TAG, "Overlay shown: $remainingSeconds / $totalSeconds seconds")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay: ${e.message}", e)
        }
    }

    /**
     * 오버레이 화면 숨김
     */
    private fun hideOverlay() {
        if (!isOverlayShowing || overlayView == null) {
            return
        }

        try {
            windowManager?.removeView(overlayView)
            overlayView = null
            isOverlayShowing = false
            Log.d(TAG, "Overlay hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide overlay: ${e.message}", e)
        }
    }

    /**
     * 포기 버튼 처리
     */
    private fun handleGiveUp() {
        Log.d(TAG, "User gave up timer")
        // TODO: TimerViewModel에 포기 이벤트 전달
        // TODO: Week 2에서 ViewModel 통신 구조 개선
        hideOverlay()
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
        hideOverlay()
        Log.d(TAG, "LockOverlayService destroyed")
    }
}
