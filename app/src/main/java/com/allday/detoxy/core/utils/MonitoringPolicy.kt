package com.allday.detoxy.core.utils

/**
 * 디톡시 제어: 모니터링 정책 및 이벤트 로깅 정의
 *
 * 주요 기능:
 * 1. 모니터링 대상 앱 범위 정의
 * 2. 허용/차단 이벤트 로깅 정책
 * 3. Analytics 이벤트 타이밍 및 파라미터 정의
 *
 * @see [01_advanced_analytics_schema.md](../../../../../../../../docs/01_advanced_analytics_schema.md)
 */
object MonitoringPolicy {

    // ==================== 모니터링 범위 정의 ====================

    /**
     * 모니터링 대상 범위
     *
     * 디톡시 세션 중 AccessibilityService가 모니터링하는 앱 범위
     */
    enum class MonitoringScope {
        /**
         * 카테고리 앱만 모니터링
         * - 성능 최적화 (40개 앱만 추적)
         * - 배터리 소모 감소
         */
        CATEGORY_ONLY,

        /**
         * 모든 앱 모니터링
         * - 기타 앱 차단 활성화 시 필요
         * - 시스템 앱 제외
         */
        ALL_APPS;

        /**
         * 모니터링해야 하는 앱인지 확인
         *
         * @param packageName 패키지명
         * @return true: 모니터링 대상, false: 무시
         */
        fun shouldMonitor(packageName: String): Boolean {
            // 시스템 앱은 항상 모니터링 제외
            if (AppCategoryMapper.isSystemApp(packageName)) return false

            return when (this) {
                CATEGORY_ONLY -> {
                    // 카테고리에 매핑된 앱만 모니터링
                    AppCategoryMapper.getCategoryByPackage(packageName) != null
                }
                ALL_APPS -> {
                    // 모든 앱 모니터링 (기타 앱 차단 활성화 시)
                    true
                }
            }
        }
    }

    /**
     * 현재 모니터링 범위 결정
     *
     * @param otherAppsEnabled 기타 앱 차단 여부
     * @return MonitoringScope (CATEGORY_ONLY 또는 ALL_APPS)
     */
    fun getCurrentScope(otherAppsEnabled: Boolean): MonitoringScope {
        return if (otherAppsEnabled) {
            MonitoringScope.ALL_APPS
        } else {
            MonitoringScope.CATEGORY_ONLY
        }
    }

    // ==================== 이벤트 로깅 정책 ====================

    /**
     * 로깅 이벤트 타입 (디톡시 세션 중)
     */
    enum class LoggingEvent {
        /**
         * 차단 이벤트 (session_interrupted)
         * - 차단된 앱 실행 시도
         * - FocusAccessibilityService에서 발생
         */
        APP_BLOCKED,

        /**
         * 허용 이벤트 (기록용, Analytics 미전송)
         * - 허용된 앱 실행
         * - 향후 UsageStats 연동 시 활용
         */
        APP_ALLOWED,

        /**
         * 세션 시작 (session_started)
         * - 카테고리별 차단 설정 포함
         */
        SESSION_START,

        /**
         * 세션 완료 (session_completed)
         * - 차단 이벤트 통계 포함
         */
        SESSION_COMPLETE,

        /**
         * 세션 포기 (session_give_up)
         * - 경과 시간, 주요 방해요인, 차단 횟수
         */
        SESSION_GIVE_UP;

        /**
         * Analytics 전송 여부
         */
        fun shouldSendToAnalytics(): Boolean = when (this) {
            APP_ALLOWED -> false  // 허용 이벤트는 로컬 기록만
            else -> true
        }
    }

    // ==================== 차단 이벤트 파라미터 ====================

    /**
     * session_interrupted 이벤트 파라미터
     *
     * @param sessionId 세션 ID (UUID)
     * @param category 차단된 앱 카테고리
     * @param elapsedSeconds 경과 시간 (초)
     * @param packageName 패키지명 (선택적, 디버그용)
     */
    data class BlockedAppEvent(
        val sessionId: String,
        val category: AppCategory,
        val elapsedSeconds: Int,
        val packageName: String? = null  // Analytics에는 전송 안 함 (프라이버시)
    ) {
        /**
         * Analytics 파라미터 Map 생성
         */
        fun toAnalyticsParams(): Map<String, Any> {
            return mapOf(
                "session_id" to sessionId,
                "category" to category.name.lowercase(),
                "elapsed_seconds" to elapsedSeconds
            )
        }
    }

    /**
     * 허용된 앱 이벤트 (로컬 기록용)
     *
     * @param packageName 패키지명
     * @param category 카테고리 (null이면 기타 앱)
     * @param timestamp 타임스탬프
     */
    data class AllowedAppEvent(
        val packageName: String,
        val category: AppCategory?,
        val timestamp: Long = System.currentTimeMillis()
    )

    // ==================== 세션 이벤트 파라미터 ====================

    /**
     * session_started 이벤트 파라미터 (확장)
     *
     * @param durationMinutes 목표 시간 (분)
     * @param enabledCategories 차단 활성화된 카테고리
     * @param otherAppsEnabled 기타 앱 차단 여부
     */
    data class SessionStartEvent(
        val durationMinutes: Int,
        val enabledCategories: Set<AppCategory>,
        val otherAppsEnabled: Boolean
    ) {
        fun toAnalyticsParams(): Map<String, Any> {
            return mapOf(
                "duration_minutes" to durationMinutes,
                "sns_enabled" to (AppCategory.SNS in enabledCategories),
                "messenger_enabled" to (AppCategory.MESSENGER in enabledCategories),
                "web_enabled" to (AppCategory.WEB in enabledCategories),
                "video_enabled" to (AppCategory.VIDEO_SHORTS in enabledCategories),
                "other_enabled" to otherAppsEnabled,
                "total_enabled_count" to enabledCategories.size
            )
        }
    }

    /**
     * session_give_up 이벤트 파라미터 (확장)
     *
     * @param durationMinutes 목표 시간 (분)
     * @param elapsedSeconds 경과 시간 (초)
     * @param interruptionCount 차단 이벤트 수
     * @param primaryDistraction 주요 방해요인 (가장 많이 차단된 카테고리)
     */
    data class SessionGiveUpEvent(
        val durationMinutes: Int,
        val elapsedSeconds: Int,
        val interruptionCount: Int,
        val primaryDistraction: AppCategory?
    ) {
        fun toAnalyticsParams(): Map<String, Any> {
            return buildMap {
                put("duration_minutes", durationMinutes)
                put("elapsed_seconds", elapsedSeconds)
                put("interruption_count", interruptionCount)
                primaryDistraction?.let {
                    put("primary_distraction", it.name.lowercase())
                }
            }
        }
    }

    // ==================== 디톡시 제어 설정 이벤트 ====================

    /**
     * detoxy_settings_saved 이벤트 파라미터
     *
     * @param preset 선택한 프리셋 ("complete_block", "standard", "relaxed", "custom")
     * @param enabledCategories 활성화된 카테고리
     * @param otherAppsEnabled 기타 앱 차단 여부
     * @param riskIndex 현재 위험 지수 (0-100, 선택적)
     */
    data class SettingsSavedEvent(
        val preset: String,
        val enabledCategories: Set<AppCategory>,
        val otherAppsEnabled: Boolean,
        val riskIndex: Int? = null
    ) {
        fun toAnalyticsParams(): Map<String, Any> {
            return buildMap {
                put("preset", preset)
                put("sns_enabled", AppCategory.SNS in enabledCategories)
                put("messenger_enabled", AppCategory.MESSENGER in enabledCategories)
                put("web_enabled", AppCategory.WEB in enabledCategories)
                put("video_enabled", AppCategory.VIDEO_SHORTS in enabledCategories)
                put("other_enabled", otherAppsEnabled)
                put("total_enabled_count", enabledCategories.size)
                riskIndex?.let { put("risk_index", it) }
            }
        }
    }

    // ==================== 통계 유틸 ====================

    /**
     * 차단 이벤트 리스트에서 주요 방해요인(가장 많이 차단된 카테고리) 추출
     *
     * @param blockedEvents 차단 이벤트 리스트
     * @return 주요 방해요인 카테고리 (없으면 null)
     */
    fun getPrimaryDistraction(blockedEvents: List<BlockedAppEvent>): AppCategory? {
        return blockedEvents
            .groupBy { it.category }
            .maxByOrNull { it.value.size }
            ?.key
    }

    /**
     * 카테고리별 차단 횟수 집계
     *
     * @param blockedEvents 차단 이벤트 리스트
     * @return 카테고리별 차단 횟수 Map
     */
    fun getBlockCountsByCategory(blockedEvents: List<BlockedAppEvent>): Map<AppCategory, Int> {
        return blockedEvents
            .groupBy { it.category }
            .mapValues { it.value.size }
    }

    // ==================== 로깅 가이드 ====================

    /**
     * 이벤트 로깅 시 주의사항
     *
     * 1. 개인정보 보호:
     *    - 패키지명은 Analytics에 전송하지 않음 (카테고리만 전송)
     *    - 로컬 DB에만 저장 (FocusInterruption, FocusDistraction)
     *
     * 2. 로깅 타이밍:
     *    - session_started: 타이머 시작 직후
     *    - session_interrupted: 차단 이벤트 발생 시 (즉시)
     *    - session_give_up: 포기 버튼 클릭 시
     *    - session_completed: 타이머 종료 시
     *
     * 3. 로깅 빈도 제한:
     *    - session_interrupted: 제한 없음 (모든 차단 이벤트 기록)
     *    - 단, 같은 앱 연속 차단 시 1초 디바운싱 적용 (선택적)
     *
     * 4. 배터리 최적화:
     *    - 카테고리 앱만 모니터링 (기본값)
     *    - 기타 앱 차단 활성화 시에만 전체 앱 모니터링
     */
    const val LOGGING_GUIDELINES = """
디톡시 제어 이벤트 로깅 가이드
1. 개인정보: 패키지명은 로컬 DB만, Analytics는 카테고리만
2. 타이밍: 즉시 로깅 (session_interrupted)
3. 빈도: 모든 차단 이벤트 기록
4. 성능: 카테고리 앱만 모니터링 (기본)
"""
}

