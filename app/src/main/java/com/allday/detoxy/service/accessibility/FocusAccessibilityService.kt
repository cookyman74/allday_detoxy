package com.allday.detoxy.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.allday.detoxy.service.overlay.LockOverlayService
import dagger.hilt.android.AndroidEntryPoint

/**
 * 앱 차단을 위한 AccessibilityService
 *
 * 사용자가 차단된 앱을 실행하려고 할 때 홈 화면으로 이동시킵니다.
 * 타이머가 실행 중일 때만 차단 기능이 활성화됩니다.
 *
 * @see AccessibilityService
 */
@AndroidEntryPoint
class FocusAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FocusAccessibilityService"

        /**
         * 차단할 앱의 패키지명 목록 (MVP 하드코딩)
         *
         * Instagram, TikTok, YouTube, Facebook, Chrome을 기본으로 차단
         */
        private val BLOCKED_APPS = setOf(
            "com.instagram.android",           // Instagram
            "com.zhiliaoapp.musically",        // TikTok
            "com.google.android.youtube",      // YouTube
            "com.facebook.katana",             // Facebook
            "com.android.chrome"               // Chrome (테스트용)
        )

        /**
         * 타이머 실행 상태를 저장하는 정적 변수
         * TODO: Week 2에서 StateFlow로 변경하여 ViewModel과 연동
         */
        @Volatile
        var isTimerRunning: Boolean = false

        /**
         * 타이머 정보 (LockOverlayScreen에 표시할 데이터)
         */
        @Volatile
        var remainingSeconds: Int = 0

        @Volatile
        var totalSeconds: Int = 0
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            Log.d(TAG, "Received null event")
            return
        }

        // 타이머 실행 상태 로그
        Log.d(TAG, "Event received - Timer running: $isTimerRunning, Event type: ${event.eventType}, Package: ${event.packageName}")

        if (!isTimerRunning) return  // 타이머가 실행 중이 아니면 차단하지 않음

        // TYPE_WINDOW_STATE_CHANGED 이벤트만 처리
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        Log.d(TAG, "Checking package: $packageName")

        // 차단 앱인지 확인
        if (packageName in BLOCKED_APPS) {
            Log.w(TAG, "⚠️ BLOCKED APP DETECTED: $packageName")
            navigateToHome()
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "AccessibilityService interrupted")
    }

    /**
     * 차단된 앱 실행 시 처리
     *
     * 1. LockOverlayScreen을 전체 화면으로 표시
     * 2. 차단된 앱을 종료하기 위해 홈 화면으로 이동
     */
    private fun navigateToHome() {
        // 1. LockOverlayScreen 표시
        LockOverlayService.showOverlay(
            context = applicationContext,
            remainingSeconds = remainingSeconds,
            totalSeconds = totalSeconds
        )
        Log.d(TAG, "Lock overlay shown: $remainingSeconds / $totalSeconds seconds")

        // 2. 홈 화면으로 이동 (차단된 앱 종료)
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            startActivity(homeIntent)
            Log.d(TAG, "Navigated to home screen")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to navigate to home: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AccessibilityService destroyed")
    }
}
