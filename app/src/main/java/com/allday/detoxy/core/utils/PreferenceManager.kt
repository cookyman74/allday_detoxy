package com.allday.detoxy.core.utils

import android.content.Context

/**
 * SharedPreferences 관리 유틸리티
 *
 * 앱의 초기 실행 여부, 사용자 설정 등을 저장합니다.
 */
class PreferenceManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "app_prefs"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_PENDING_SUCCESS_ANIMATION = "pending_success_animation" // 🆕 성공 애니메이션 대기 플래그
        private const val KEY_LAST_TIMER_STYLE_INDEX = "last_timer_style_index" // 🆕 마지막 타이머 스타일
        
        // 🆕 v9: 오버레이 프라이버시 설정 키
        private const val KEY_SHOW_TODO_OVERLAY = "show_todo_overlay"
        private const val KEY_SHOW_DETAILED_TODO_OVERLAY = "show_detailed_todo_overlay"
        private const val KEY_HIDE_GOAL_OVERLAY = "hide_goal_overlay"
    }

    /**
     * 마지막 선택한 타이머 스타일 로드
     */
    fun getLastTimerStyleIndex(): Int {
        return prefs.getInt(KEY_LAST_TIMER_STYLE_INDEX, 0)
    }

    /**
     * 마지막 선택한 타이머 스타일 저장
     */
    fun setLastTimerStyleIndex(index: Int) {
        prefs.edit().putInt(KEY_LAST_TIMER_STYLE_INDEX, index).apply()
    }

    /**
     * 성공 애니메이션 대기 상태 확인
     */
    fun hasPendingSuccessAnimation(): Boolean {
        return prefs.getBoolean(KEY_PENDING_SUCCESS_ANIMATION, false)
    }

    /**
     * 성공 애니메이션 대기 상태 설정
     */
    fun setPendingSuccessAnimation(pending: Boolean) {
        prefs.edit().putBoolean(KEY_PENDING_SUCCESS_ANIMATION, pending).apply()
    }

    /**
     * 앱이 처음 실행되는지 확인
     *
     * @return 첫 실행이면 true, 아니면 false
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * 첫 실행 완료 플래그 설정
     *
     * 권한 안내 화면을 완료했을 때 호출됩니다.
     */
    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    /**
     * 첫 실행 플래그 초기화 (테스트용)
     *
     * 디버깅 목적으로 첫 실행 상태로 되돌립니다.
     */
    fun resetFirstLaunch() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, true).apply()
    }

    /**
     * 온보딩 완료 여부 확인
     *
     * @return 온보딩을 완료했으면 true, 아니면 false
     */
    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    /**
     * 온보딩 완료 플래그 설정
     *
     * 환영 화면을 완료했을 때 호출됩니다.
     */
    fun setOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }

    /**
     * 온보딩 플래그 초기화 (테스트용)
     *
     * 디버깅 목적으로 온보딩 미완료 상태로 되돌립니다.
     */
    fun resetOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, false).apply()
    }
    
    // ========================================
    // 🆕 v9: 오버레이 프라이버시 설정
    // ========================================
    
    /**
     * 오버레이에 목표/할일 표시 여부
     * true: 목표 표시 (기본값)
     * false: 목표 숨김
     */
    var showTodoOnOverlay: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TODO_OVERLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_TODO_OVERLAY, value).apply()
    
    /**
     * 오버레이에 상세 할일 목록 표시 여부
     * true: 목표 + 할일 목록 표시
     * false: 목표만 표시 (기본값)
     */
    var showDetailedTodoOnOverlay: Boolean
        get() = prefs.getBoolean(KEY_SHOW_DETAILED_TODO_OVERLAY, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_DETAILED_TODO_OVERLAY, value).apply()
    
    /**
     * 오버레이에서 목표 숨기기 (이모지만 표시)
     * true: 🎯 이모지만 표시 (프라이버시 보호)
     * false: 텍스트 표시 (기본값)
     */
    var hideGoalOnOverlay: Boolean
        get() = prefs.getBoolean(KEY_HIDE_GOAL_OVERLAY, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_GOAL_OVERLAY, value).apply()
}

