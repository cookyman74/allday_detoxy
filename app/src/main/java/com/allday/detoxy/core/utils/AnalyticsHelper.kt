package com.allday.detoxy.core.utils

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

/**
 * Firebase Analytics 이벤트 로깅 Helper
 *
 * 디톡시 제어 설정 및 세션 관련 이벤트를 로깅합니다.
 *
 * @see [01_advanced_analytics_schema.md](../../../../../../../../docs/01_advanced_analytics_schema.md)
 */
object AnalyticsHelper {

    private const val TAG = "AnalyticsHelper"

    // Firebase Analytics 인스턴스 (lazy initialization)
    private var firebaseAnalytics: FirebaseAnalytics? = null

    /**
     * Analytics 초기화
     *
     * Application.onCreate()에서 호출
     */
    fun initialize(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        Log.d(TAG, "Firebase Analytics initialized")
    }

    // ==================== 디톡시 제어 설정 이벤트 ====================

    /**
     * detoxy_settings_open 이벤트
     *
     * 디톡시 제어 설정 화면 진입
     *
     * @param source 진입 경로 ("timer_screen", "settings_menu")
     */
    fun logDetoxySettingsOpen(source: String) {
        val bundle = Bundle().apply {
            putString("source", source)
        }
        logEvent("detoxy_settings_open", bundle)
    }

    /**
     * detoxy_settings_saved 이벤트
     *
     * 디톡시 제어 설정 저장
     *
     * @param preset 선택한 프리셋 ("complete_block", "standard", "relaxed", "custom")
     * @param enabledCategories 활성화된 카테고리 Set
     * @param otherAppsEnabled 기타 앱 차단 여부
     * @param riskIndex 현재 위험 지수 (0-100, 선택적)
     */
    fun logDetoxySettingsSaved(
        preset: String,
        enabledCategories: Set<AppCategory>,
        otherAppsEnabled: Boolean,
        riskIndex: Int? = null
    ) {
        val bundle = Bundle().apply {
            putString("preset", preset)
            putBoolean("sns_enabled", AppCategory.SNS in enabledCategories)
            putBoolean("messenger_enabled", AppCategory.MESSENGER in enabledCategories)
            putBoolean("web_enabled", AppCategory.WEB in enabledCategories)
            putBoolean("video_enabled", AppCategory.VIDEO_SHORTS in enabledCategories)
            putBoolean("other_enabled", otherAppsEnabled)
            putInt("total_enabled_count", enabledCategories.size)
            riskIndex?.let { putInt("risk_index", it) }
        }
        logEvent("detoxy_settings_saved", bundle)
    }

    /**
     * detoxy_settings_category_toggle 이벤트
     *
     * 카테고리 토글 변경
     *
     * @param category 카테고리
     * @param enabled 활성화 여부
     * @param dialogShown 메신저 안내 다이얼로그 표시 여부 (메신저 카테고리만)
     */
    fun logDetoxySettingsCategoryToggle(
        category: AppCategory,
        enabled: Boolean,
        dialogShown: Boolean = false
    ) {
        val bundle = Bundle().apply {
            putString("category", category.name.lowercase())
            putBoolean("enabled", enabled)
            if (category == AppCategory.MESSENGER) {
                putBoolean("dialog_shown", dialogShown)
            }
        }
        logEvent("detoxy_settings_category_toggle", bundle)
    }

    /**
     * detoxy_settings_preset_selected 이벤트
     *
     * 프리셋 선택
     *
     * @param preset 선택한 프리셋
     * @param previousPreset 이전 프리셋 (없으면 null)
     */
    fun logDetoxySettingsPresetSelected(
        preset: AppCategoryMapper.DetoxyPreset,
        previousPreset: AppCategoryMapper.DetoxyPreset?
    ) {
        val presetName = when (preset) {
            AppCategoryMapper.DetoxyPreset.COMPLETE_BLOCK -> "complete_block"
            AppCategoryMapper.DetoxyPreset.STANDARD_DETOXY -> "standard"
            AppCategoryMapper.DetoxyPreset.RELAXED -> "relaxed"
        }

        val previousName = previousPreset?.let {
            when (it) {
                AppCategoryMapper.DetoxyPreset.COMPLETE_BLOCK -> "complete_block"
                AppCategoryMapper.DetoxyPreset.STANDARD_DETOXY -> "standard"
                AppCategoryMapper.DetoxyPreset.RELAXED -> "relaxed"
            }
        } ?: "none"

        val bundle = Bundle().apply {
            putString("preset", presetName)
            putString("previous_preset", previousName)
        }
        logEvent("detoxy_settings_preset_selected", bundle)
    }

    // ==================== 세션 이벤트 (확장) ====================

    /**
     * session_started 이벤트 (확장)
     *
     * 세션 시작 시 카테고리별 차단 설정 포함
     *
     * @param durationMinutes 목표 시간 (분)
     * @param enabledCategories 활성화된 카테고리
     * @param otherAppsEnabled 기타 앱 차단 여부
     */
    fun logSessionStarted(
        durationMinutes: Int,
        enabledCategories: Set<AppCategory>,
        otherAppsEnabled: Boolean
    ) {
        val bundle = Bundle().apply {
            putInt("duration_minutes", durationMinutes)
            putBoolean("sns_enabled", AppCategory.SNS in enabledCategories)
            putBoolean("messenger_enabled", AppCategory.MESSENGER in enabledCategories)
            putBoolean("web_enabled", AppCategory.WEB in enabledCategories)
            putBoolean("video_enabled", AppCategory.VIDEO_SHORTS in enabledCategories)
            putBoolean("other_enabled", otherAppsEnabled)
            putInt("total_enabled_count", enabledCategories.size)
        }
        logEvent("session_started", bundle)
    }

    /**
     * session_interrupted 이벤트 (v2)
     *
     * 세션 중 차단 이벤트
     *
     * @param category 차단된 앱 카테고리
     * @param remainingSeconds 남은 시간 (초)
     */
    fun logSessionInterrupted(
        category: String,
        remainingSeconds: Int
    ) {
        val bundle = Bundle().apply {
            putString("category", category.lowercase())
            putInt("remaining_seconds", remainingSeconds)
        }
        logEvent("session_interrupted", bundle)
    }

    /**
     * session_give_up 이벤트 (확장)
     *
     * 세션 중도 포기
     *
     * @param durationMinutes 목표 시간 (분)
     * @param elapsedSeconds 경과 시간 (초)
     * @param interruptionCount 차단 이벤트 수
     * @param primaryDistraction 주요 방해요인 카테고리
     */
    fun logSessionGiveUp(
        durationMinutes: Int,
        elapsedSeconds: Int,
        interruptionCount: Int,
        primaryDistraction: AppCategory?
    ) {
        val bundle = Bundle().apply {
            putInt("duration_minutes", durationMinutes)
            putInt("elapsed_seconds", elapsedSeconds)
            putInt("interruption_count", interruptionCount)
            primaryDistraction?.let {
                putString("primary_distraction", it.name.lowercase())
            }
        }
        logEvent("session_give_up", bundle)
    }

    // ==================== 2차 고도화: 자동 실행 설정 이벤트 ====================

    /**
     * auto_run_time_created 이벤트
     *
     * 시간 기반 자동 실행 생성
     */
    fun logTimeBasedAutoRunCreated(
        hour: Int,
        minute: Int,
        durationMinutes: Int,
        presetType: String,
        enabledDaysCount: Int,
        hasLabel: Boolean,
        isFromTemplate: Boolean,
        templateType: String?
    ) {
        val bundle = Bundle().apply {
            putInt("hour", hour)
            putInt("minute", minute)
            putInt("duration_minutes", durationMinutes)
            putString("preset_type", presetType)
            putInt("enabled_days_count", enabledDaysCount)
            putBoolean("has_label", hasLabel)
            putBoolean("is_from_template", isFromTemplate)
            templateType?.let { putString("template_type", it) }
        }
        logEvent("auto_run_time_created", bundle)
    }

    /**
     * auto_run_time_edited 이벤트
     *
     * 시간 기반 자동 실행 편집
     */
    fun logTimeBasedAutoRunEdited(
        changedFields: String,
        newDurationMinutes: Int?,
        newEnabledDaysCount: Int?
    ) {
        val bundle = Bundle().apply {
            putString("changed_fields", changedFields)
            newDurationMinutes?.let { putInt("new_duration_minutes", it) }
            newEnabledDaysCount?.let { putInt("new_enabled_days_count", it) }
        }
        logEvent("auto_run_time_edited", bundle)
    }

    /**
     * auto_run_time_toggled 이벤트
     *
     * 시간 기반 자동 실행 활성화/비활성화
     */
    fun logTimeBasedAutoRunToggled(
        isEnabled: Boolean,
        totalEnabledCount: Int
    ) {
        val bundle = Bundle().apply {
            putBoolean("is_enabled", isEnabled)
            putInt("total_enabled_count", totalEnabledCount)
        }
        logEvent("auto_run_time_toggled", bundle)
    }

    /**
     * auto_run_time_deleted 이벤트
     *
     * 시간 기반 자동 실행 삭제
     */
    fun logTimeBasedAutoRunDeleted(
        usageCount: Int,
        daysActive: Int
    ) {
        val bundle = Bundle().apply {
            putInt("usage_count", usageCount)
            putInt("days_active", daysActive)
        }
        logEvent("auto_run_time_deleted", bundle)
    }

    /**
     * auto_run_location_created 이벤트
     *
     * 위치 기반 자동 실행 생성
     */
    fun logLocationBasedAutoRunCreated(
        locationLabelHash: String,
        radiusMeters: Int,
        durationMinutes: Int,
        presetType: String,
        triggerType: String,
        dwellTimeMinutes: Int,
        requiresConfirmation: Boolean
    ) {
        val bundle = Bundle().apply {
            putString("location_label_hash", locationLabelHash)
            putInt("radius_meters", radiusMeters)
            putInt("duration_minutes", durationMinutes)
            putString("preset_type", presetType)
            putString("trigger_type", triggerType)
            putInt("dwell_time_minutes", dwellTimeMinutes)
            putBoolean("requires_confirmation", requiresConfirmation)
        }
        logEvent("auto_run_location_created", bundle)
    }

    /**
     * auto_run_location_edited 이벤트
     *
     * 위치 기반 자동 실행 편집
     */
    fun logLocationBasedAutoRunEdited(
        changedFields: String,
        newRadiusMeters: Int?,
        newDwellTimeMinutes: Int?
    ) {
        val bundle = Bundle().apply {
            putString("changed_fields", changedFields)
            newRadiusMeters?.let { putInt("new_radius_meters", it) }
            newDwellTimeMinutes?.let { putInt("new_dwell_time_minutes", it) }
        }
        logEvent("auto_run_location_edited", bundle)
    }

    /**
     * auto_run_location_toggled 이벤트
     *
     * 위치 기반 자동 실행 활성화/비활성화
     */
    fun logLocationBasedAutoRunToggled(
        isEnabled: Boolean,
        totalEnabledCount: Int
    ) {
        val bundle = Bundle().apply {
            putBoolean("is_enabled", isEnabled)
            putInt("total_enabled_count", totalEnabledCount)
        }
        logEvent("auto_run_location_toggled", bundle)
    }

    /**
     * auto_run_location_deleted 이벤트
     *
     * 위치 기반 자동 실행 삭제
     */
    fun logLocationBasedAutoRunDeleted(
        usageCount: Int,
        successRate: Int,
        daysActive: Int
    ) {
        val bundle = Bundle().apply {
            putInt("usage_count", usageCount)
            putInt("success_rate", successRate)
            putInt("days_active", daysActive)
        }
        logEvent("auto_run_location_deleted", bundle)
    }

    // ==================== 2차 고도화: 자동 실행 트리거 및 결과 이벤트 ====================

    /**
     * auto_run_triggered 이벤트
     *
     * 자동 실행 트리거 발생
     */
    fun logAutoRunTriggered(
        triggerType: String,
        sourceIdHash: String,
        durationMinutes: Int,
        presetType: String
    ) {
        val bundle = Bundle().apply {
            putString("trigger_type", triggerType)
            putString("source_id_hash", sourceIdHash)
            putInt("duration_minutes", durationMinutes)
            putString("preset_type", presetType)
        }
        logEvent("auto_run_triggered", bundle)
    }

    /**
     * auto_run_notification_shown 이벤트
     *
     * 자동 실행 알림 표시
     */
    fun logAutoRunNotificationShown(
        triggerType: String,
        isPreNotification: Boolean,
        minutesBefore: Int?
    ) {
        val bundle = Bundle().apply {
            putString("trigger_type", triggerType)
            putBoolean("is_pre_notification", isPreNotification)
            minutesBefore?.let { putInt("minutes_before", it) }
        }
        logEvent("auto_run_notification_shown", bundle)
    }

    /**
     * auto_run_notification_action 이벤트
     *
     * 자동 실행 알림 액션
     */
    fun logAutoRunNotificationAction(
        action: String,
        triggerType: String,
        responseTimeSeconds: Int
    ) {
        val bundle = Bundle().apply {
            putString("action", action)
            putString("trigger_type", triggerType)
            putInt("response_time_seconds", responseTimeSeconds)
        }
        logEvent("auto_run_notification_action", bundle)
    }

    /**
     * auto_run_started 이벤트
     *
     * 자동 실행으로 타이머 시작
     */
    fun logAutoRunStarted(
        triggerType: String,
        durationMinutes: Int,
        isAutoStart: Boolean,
        delaySeconds: Int,
        gpsAccuracyMeters: Float? = null,
        dwellSeconds: Int? = null
    ) {
        val bundle = Bundle().apply {
            putString("trigger_type", triggerType)
            putInt("duration_minutes", durationMinutes)
            putBoolean("is_auto_start", isAutoStart)
            putInt("delay_seconds", delaySeconds)
            gpsAccuracyMeters?.let { putDouble("gps_accuracy_meters", it.toDouble()) }
            dwellSeconds?.let { putInt("dwell_seconds", it) }
        }
        logEvent("auto_run_started", bundle)
    }

    /**
     * auto_run_skipped 이벤트
     *
     * 자동 실행 건너뜀
     */
    fun logAutoRunSkipped(
        triggerType: String,
        reason: String
    ) {
        val bundle = Bundle().apply {
            putString("trigger_type", triggerType)
            putString("reason", reason)
        }
        logEvent("auto_run_skipped", bundle)
    }

    /**
     * auto_run_failed 이벤트
     *
     * 자동 실행 실패
     */
    fun logAutoRunFailed(
        triggerType: String,
        failureReason: String,
        gpsAccuracyMeters: Float? = null
    ) {
        val bundle = Bundle().apply {
            putString("trigger_type", triggerType)
            putString("failure_reason", failureReason)
            gpsAccuracyMeters?.let { putDouble("gps_accuracy_meters", it.toDouble()) }
        }
        logEvent("auto_run_failed", bundle)
    }

    // ==================== 2차 고도화: 커스텀 타이머 이벤트 ====================

    /**
     * custom_timer_adjusted 이벤트
     *
     * 도넛 그래프로 시간 조정
     */
    fun logCustomTimerAdjusted(
        durationMinutes: Int,
        method: String,
        isCustomTime: Boolean
    ) {
        val bundle = Bundle().apply {
            putInt("duration_minutes", durationMinutes)
            putString("method", method)
            putBoolean("is_custom_time", isCustomTime)
        }
        logEvent("custom_timer_adjusted", bundle)
    }

    /**
     * custom_preset_created 이벤트
     *
     * 커스텀 프리셋 생성
     */
    fun logCustomPresetCreated(
        nameLength: Int,
        durationMinutes: Int,
        hasPresetType: Boolean,
        totalCustomCount: Int
    ) {
        val bundle = Bundle().apply {
            putInt("name_length", nameLength)
            putInt("duration_minutes", durationMinutes)
            putBoolean("has_preset_type", hasPresetType)
            putInt("total_custom_count", totalCustomCount)
        }
        logEvent("custom_preset_created", bundle)
    }

    /**
     * custom_preset_used 이벤트
     *
     * 커스텀 프리셋 사용
     */
    fun logCustomPresetUsed(
        presetIdHash: String,
        durationMinutes: Int,
        usageCount: Int
    ) {
        val bundle = Bundle().apply {
            putString("preset_id_hash", presetIdHash)
            putInt("duration_minutes", durationMinutes)
            putInt("usage_count", usageCount)
        }
        logEvent("custom_preset_used", bundle)
    }

    /**
     * custom_preset_edited 이벤트
     *
     * 커스텀 프리셋 편집
     */
    fun logCustomPresetEdited(
        changedFields: String,
        newDurationMinutes: Int?,
        newNameLength: Int?
    ) {
        val bundle = Bundle().apply {
            putString("changed_fields", changedFields)
            newDurationMinutes?.let { putInt("new_duration_minutes", it) }
            newNameLength?.let { putInt("new_name_length", it) }
        }
        logEvent("custom_preset_edited", bundle)
    }

    /**
     * custom_preset_deleted 이벤트
     *
     * 커스텀 프리셋 삭제
     */
    fun logCustomPresetDeleted(
        usageCount: Int,
        daysActive: Int
    ) {
        val bundle = Bundle().apply {
            putInt("usage_count", usageCount)
            putInt("days_active", daysActive)
        }
        logEvent("custom_preset_deleted", bundle)
    }

    // ==================== 2차 고도화: 권한 이벤트 ====================

    /**
     * permission_exact_alarm_requested 이벤트
     *
     * 정확 알람 권한 요청 (Android 12+)
     */
    fun logPermissionExactAlarmRequested(
        source: String,
        isGranted: Boolean,
        fallbackToWorkmanager: Boolean,
        wentToSettings: Boolean
    ) {
        val bundle = Bundle().apply {
            putString("source", source)
            putBoolean("is_granted", isGranted)
            putBoolean("fallback_to_workmanager", fallbackToWorkmanager)
            putBoolean("went_to_settings", wentToSettings)
        }
        logEvent("permission_exact_alarm_requested", bundle)
    }

    /**
     * permission_background_location_requested 이벤트
     *
     * 백그라운드 위치 권한 요청
     */
    fun logPermissionBackgroundLocationRequested(
        source: String,
        isGranted: Boolean,
        permissionLevel: String
    ) {
        val bundle = Bundle().apply {
            putString("source", source)
            putBoolean("is_granted", isGranted)
            putString("permission_level", permissionLevel)
        }
        logEvent("permission_background_location_requested", bundle)
    }

    // ==================== 내부 헬퍼 ====================

    /**
     * Firebase Analytics 이벤트 로깅
     *
     * @param eventName 이벤트 이름
     * @param bundle 파라미터 Bundle
     */
    private fun logEvent(eventName: String, bundle: Bundle) {
        try {
            firebaseAnalytics?.logEvent(eventName, bundle)
            Log.d(TAG, "Event logged: $eventName (${bundle.size()} params)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log event: $eventName", e)
        }
    }

    // ==================== 로깅 가이드 ====================

    /**
     * Analytics 로깅 시 주의사항
     *
     * 1. 개인정보 보호:
     *    - 패키지명은 로깅하지 않음 (카테고리만 로깅)
     *    - 세션 ID는 UUID (개인 식별 불가)
     *
     * 2. 이벤트 파라미터 제한:
     *    - Firebase Analytics: 최대 25개 파라미터
     *    - 파라미터 이름: 최대 40자
     *    - 파라미터 값: 최대 100자 (문자열)
     *
     * 3. 이벤트 이름 규칙:
     *    - snake_case 사용
     *    - 최대 40자
     *    - 영문 소문자, 숫자, 언더스코어만 사용
     *
     * 4. 로깅 빈도:
     *    - session_interrupted: 차단 이벤트마다 로깅
     *    - 기타: 사용자 액션 시에만 로깅
     */
}

