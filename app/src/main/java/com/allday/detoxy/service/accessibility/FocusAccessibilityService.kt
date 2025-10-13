package com.allday.detoxy.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.allday.detoxy.core.utils.AppCategory
import com.allday.detoxy.core.utils.AppCategoryMapper
import com.allday.detoxy.service.overlay.LockOverlayService
import dagger.hilt.android.AndroidEntryPoint

/**
 * 앱 차단을 위한 AccessibilityService (디톡시 제어)
 *
 * 주요 기능:
 * 1. 동적 카테고리 기반 앱 차단 (AppCategoryMapper 사용)
 * 2. 차단 이벤트 감지 및 LockOverlayScreen 표시
 * 3. 타이머 실행 중에만 차단 기능 활성화
 *
 * @see AccessibilityService
 * @see AppCategoryMapper
 */
@AndroidEntryPoint
class FocusAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FocusAccessibilityService"

        /**
         * 타이머 실행 상태
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

        /**
         * 디톡시 제어 설정 (동적 차단 목록)
         *
         * MVP: 기본값은 표준 디톡시 프리셋 (SNS, WEB, VIDEO_SHORTS)
         * 1차 고도화: FocusSettings 엔티티에서 로드
         */
        @Volatile
        var enabledCategories: Set<AppCategory> = setOf(
            AppCategory.SNS,
            AppCategory.WEB,
            AppCategory.VIDEO_SHORTS
        )

        @Volatile
        var otherAppsEnabled: Boolean = false

        /**
         * 차단 설정 업데이트 (TimerViewModel에서 호출)
         *
         * @param categories 차단할 카테고리 Set
         * @param blockOtherApps 기타 앱 차단 여부
         */
        fun updateBlockSettings(categories: Set<AppCategory>, blockOtherApps: Boolean) {
            enabledCategories = categories
            otherAppsEnabled = blockOtherApps
            Log.i(TAG, "🔧 Block settings updated: $categories, otherApps=$blockOtherApps")
        }

        /**
         * 프리셋 적용 (빠른 설정)
         *
         * @param preset 적용할 프리셋
         */
        fun applyPreset(preset: AppCategoryMapper.DetoxyPreset) {
            val (categories, otherApps) = AppCategoryMapper.applyPreset(preset)
            updateBlockSettings(categories, otherApps)
        }
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
        Log.d(TAG, "Event received - Timer: $isTimerRunning, Type: ${event.eventType}, Package: ${event.packageName}")

        if (!isTimerRunning) return  // 타이머가 실행 중이 아니면 차단하지 않음

        // TYPE_WINDOW_STATE_CHANGED 이벤트만 처리
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // 디톡시 제어 설정 기반 차단 여부 확인
        if (isAppBlocked(packageName)) {
            val category = AppCategoryMapper.getCategoryByPackage(packageName)
            Log.w(TAG, "⚠️ BLOCKED APP DETECTED: $packageName (Category: ${category?.getDisplayName() ?: "OTHER"})")
            handleBlockedApp(packageName, category)
        }
    }

    /**
     * 앱 차단 여부 확인 (AppCategoryMapper 사용)
     *
     * @param packageName 확인할 패키지명
     * @return true: 차단, false: 허용
     */
    private fun isAppBlocked(packageName: String): Boolean {
        return AppCategoryMapper.isBlocked(
            packageName = packageName,
            enabledCategories = enabledCategories,
            otherAppsEnabled = otherAppsEnabled
        )
    }

    /**
     * 차단된 앱 처리 (차단 이벤트 기록 + 오버레이 표시)
     *
     * @param packageName 차단된 앱 패키지명
     * @param category 앱 카테고리 (null이면 OTHER)
     */
    private fun handleBlockedApp(packageName: String, category: AppCategory?) {
        // TODO: Analytics 이벤트 로깅 (session_interrupted)
        // FirebaseAnalytics.logEvent("session_interrupted", Bundle().apply {
        //     putString("package_name", packageName)
        //     putString("category", category?.name ?: "OTHER")
        //     putInt("remaining_seconds", remainingSeconds)
        // })

        Log.i(TAG, "🚫 App blocked: $packageName (${category?.getDisplayName() ?: "기타 앱"})")

        // LockOverlayScreen 표시 및 홈 화면 이동
        navigateToHome()
    }

    override fun onInterrupt() {
        Log.d(TAG, "AccessibilityService interrupted")
    }

    /**
     * 차단된 앱 실행 시 처리
     *
     * 1. LockOverlayScreen을 전체 화면으로 표시
     * 2. 홈 화면으로 이동하여 차단된 앱 종료
     */
    private fun navigateToHome() {
        // 1. 먼저 LockOverlayScreen 표시
        LockOverlayService.showOverlay(
            context = applicationContext,
            remainingSeconds = remainingSeconds,
            totalSeconds = totalSeconds
        )
        Log.d(TAG, "🔒 Lock overlay display requested: $remainingSeconds / $totalSeconds seconds")

        // 2. 홈 화면으로 이동 (차단된 앱 종료)
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            startActivity(homeIntent)
            Log.d(TAG, "✅ Navigated to home screen - blocked app closed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to navigate to home: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AccessibilityService destroyed")
    }
}
