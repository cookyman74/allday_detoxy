package com.allday.detoxy.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.allday.detoxy.core.utils.AnalyticsHelper
import com.allday.detoxy.core.utils.AppCategory
import com.allday.detoxy.core.utils.AppCategoryMapper
import com.allday.detoxy.data.local.entity.FocusInterruption
import com.allday.detoxy.domain.repository.FocusRepository
import com.allday.detoxy.service.overlay.LockOverlayService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 앱 차단을 위한 AccessibilityService (디톡시 제어)
 *
 * 주요 기능:
 * 1. 동적 카테고리 기반 앱 차단 (AppCategoryMapper 사용)
 * 2. 차단 이벤트 감지 및 LockOverlayScreen 표시
 * 3. 타이머 실행 중에만 차단 기능 활성화
 * 4. 차단 이벤트 로깅 (FocusInterruption 엔티티) - v2
 *
 * @see AccessibilityService
 * @see AppCategoryMapper
 *
 * ⚠️ Hilt 이슈 대응:
 * AccessibilityService는 시스템이 직접 인스턴스를 생성하므로 @AndroidEntryPoint가 작동하지 않습니다.
 * EntryPoint를 사용하여 수동으로 의존성을 주입합니다.
 */
class FocusAccessibilityService : AccessibilityService() {

    /**
     * Hilt EntryPoint for manual dependency injection
     *
     * AccessibilityService는 시스템이 직접 인스턴스를 생성하므로
     * @AndroidEntryPoint를 사용할 수 없습니다.
     * EntryPointAccessors를 통해 수동으로 의존성을 가져옵니다.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FocusAccessibilityServiceEntryPoint {
        fun repository(): FocusRepository
    }

    private var repository: FocusRepository? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "FocusAccessibilityService"
        private const val BLOCK_COOLDOWN_MS = 1500L // 1.5초 쿨다운 (중복 실행 방지)

        @Volatile
        private var lastBlockTime: Long = 0

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
         * 현재 세션 ID (차단 이벤트 로깅용) - v2
         */
        @Volatile
        var currentSessionId: String? = null

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
         * 🆕 v9: 현재 스케줄 정보 JSON (목표/할일)
         * 오버레이에 표시할 스케줄 정보
         */
        @Volatile
        var currentScheduleInfoJson: String? = null

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
        try {
            // ⚠️ 중요: super.onServiceConnected() 호출 전에 모든 초기화 완료
            // 시스템이 서비스를 성공적으로 연결했다고 판단하도록 함
            
            Log.i(TAG, "🔄 [CONNECT] AccessibilityService connecting... (Thread: ${Thread.currentThread().name})")
            Log.d(TAG, "🔄 [CONNECT] Current state: isTimerRunning=$isTimerRunning, categories=${enabledCategories.size}")
            
            // 🆕 Hilt 의존성 주입 (EntryPoint 사용) - 백그라운드에서 비동기 처리
            // Hilt 초기화가 완료될 때까지 대기하여 EntryPoint 사용 가능하도록 함
            serviceScope.launch {
                try {
                    Log.d(TAG, "⏳ [CONNECT] Starting Hilt injection...")
                    
                    // Hilt 초기화 대기 (최대 5초, 100ms 간격으로 재시도)
                    var retryCount = 0
                    val maxRetries = 50
                    var injectionSuccess = false
                    
                    while (retryCount < maxRetries && !injectionSuccess) {
                        try {
                            val entryPoint = EntryPointAccessors.fromApplication(
                                applicationContext,
                                FocusAccessibilityServiceEntryPoint::class.java
                            )
                            repository = entryPoint.repository()
                            Log.i(TAG, "✅ [CONNECT] Repository injected successfully (retry: $retryCount)")
                            injectionSuccess = true
                        } catch (e: Throwable) {
                            retryCount++
                            if (retryCount < maxRetries) {
                                Log.d(TAG, "⏳ [CONNECT] Waiting for Hilt initialization... (retry: $retryCount/$maxRetries)")
                                delay(100) // 100ms 대기 후 재시도
                            } else {
                                Log.e(TAG, "❌ [CONNECT] Failed to inject repository after $maxRetries retries: ${e.message}")
                                Log.e(TAG, "❌ [CONNECT] Stack trace: ${e.stackTraceToString()}")
                                // repository가 null이어도 앱 차단 기능은 작동 (로깅만 실패)
                            }
                        }
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "❌ [CONNECT] Failed to inject repository: ${e.message}")
                    Log.e(TAG, "❌ [CONNECT] Stack trace: ${e.stackTraceToString()}")
                    // repository가 null이어도 앱 차단 기능은 작동 (로깅만 실패)
                }
            }
            
            // super.onServiceConnected() 호출 - 시스템에 서비스 연결 성공 알림
            super.onServiceConnected()
            Log.i(TAG, "✅ [CONNECT] AccessibilityService connected successfully")
            
            // 서비스 상태 로깅
            Log.i(TAG, "📊 [CONNECT] Service state: isTimerRunning=$isTimerRunning, categories=${enabledCategories.size}, otherApps=$otherAppsEnabled")
        } catch (e: Throwable) {
            Log.e(TAG, "❌ [CONNECT] CRITICAL: onServiceConnected failed: ${e.message}")
            Log.e(TAG, "❌ [CONNECT] Stack trace: ${e.stackTraceToString()}")
            // 서비스 연결 실패 시에도 크래시 방지
            // super.onServiceConnected()는 예외 발생 시 호출하지 않음 (시스템이 자동으로 처리)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) {
                return
            }

            // TYPE_WINDOW_STATE_CHANGED 이벤트만 처리
            if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

            val packageName = event.packageName?.toString() ?: return

            // 타이머 실행 상태 확인 (상세 로그)
            if (!isTimerRunning) {
                return
            }

            // 디톡시 제어 설정 기반 차단 여부 확인
            if (isAppBlocked(packageName)) {
                val category = AppCategoryMapper.getCategoryByPackage(packageName)
                Log.w(TAG, "⚠️ BLOCKED APP DETECTED: $packageName (Category: ${category?.getDisplayName() ?: "OTHER"})")
                handleBlockedApp(packageName, category)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "❌ Error in onAccessibilityEvent: ${e.message}")
            // 예외 발생 시에도 서비스가 크래시되지 않도록 처리
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
        // 🔥 중복 차단 방지 (쿨다운)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBlockTime < BLOCK_COOLDOWN_MS) {
            return
        }
        lastBlockTime = currentTime

        val categoryName = category?.name ?: "OTHER"
        
        Log.i(TAG, "🚫 App blocked: $packageName ($categoryName)")

        // 1. 차단 이벤트 로깅 (FocusInterruption 엔티티)
        currentSessionId?.let { sessionId ->
            repository?.let { repo ->
            serviceScope.launch {
                try {
                        repo.logInterruption(
                        FocusInterruption(
                            id = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            timestamp = System.currentTimeMillis(),
                            packageName = packageName,
                            category = categoryName
                        )
                    )
                    Log.d(TAG, "✅ Interruption logged: $packageName ($categoryName)")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to log interruption", e)
                }
            }
            } ?: Log.w(TAG, "⚠️ Repository is null, interruption not logged")
        } ?: Log.w(TAG, "⚠️ currentSessionId is null, interruption not logged")

        // 2. Analytics 이벤트 로깅
        AnalyticsHelper.logSessionInterrupted(
            category = categoryName,
            remainingSeconds = remainingSeconds
        )

        // 3. LockOverlayScreen 표시 및 홈 화면 이동
        navigateToHome()
    }

    override fun onInterrupt() {
        try {
            Log.w(TAG, "⚠️ [INTERRUPT] AccessibilityService interrupted (Thread: ${Thread.currentThread().name})")
            Log.w(TAG, "⚠️ [INTERRUPT] Current state: isTimerRunning=$isTimerRunning, sessionId=$currentSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [INTERRUPT] Error in onInterrupt: ${e.message}")
        }
    }

    /**
     * 차단된 앱 실행 시 처리
     *
     * 1. LockOverlayScreen을 전체 화면으로 표시
     * 2. 홈 화면으로 이동하여 차단된 앱 종료
     */
    private fun navigateToHome() {
        // 1. 먼저 LockOverlayScreen 표시 (🔥 v0.10.1.4: 예외 처리 추가)
        try {
            LockOverlayService.showOverlay(
                context = applicationContext,
                remainingSeconds = remainingSeconds,
                totalSeconds = totalSeconds,
                scheduleInfoJson = currentScheduleInfoJson  // 🆕 v9: 목표/할일 정보 전달
            )
            Log.d(TAG, "🔒 Lock overlay display requested: $remainingSeconds / $totalSeconds seconds")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to show lock overlay: ${e.message}", e)
            // 오버레이 표시 실패해도 홈 화면 이동은 계속 진행
        }

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
        try {
            super.onDestroy()
            
            // 코루틴 스코프 정리 (메모리 누수 방지)
            serviceScope.cancel()
            
            // 현재 세션 ID 초기화
            currentSessionId = null
            
            Log.i(TAG, "✅ AccessibilityService destroyed (serviceScope cancelled)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in onDestroy: ${e.message}", e)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "🔌 [UNBIND] AccessibilityService unbind (Thread: ${Thread.currentThread().name})")
        Log.i(TAG, "🔌 [UNBIND] Final state: isTimerRunning=$isTimerRunning, sessionId=$currentSessionId")
        return super.onUnbind(intent)
    }
}
