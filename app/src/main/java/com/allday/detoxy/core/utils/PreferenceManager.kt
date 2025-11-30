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
}

