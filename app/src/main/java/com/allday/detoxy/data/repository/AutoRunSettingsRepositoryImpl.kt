package com.allday.detoxy.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.allday.detoxy.domain.repository.AutoRunSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 자동 실행 글로벌 설정 Repository 구현체
 *
 * DataStore Preferences를 통해 자동 실행 글로벌 설정을 관리합니다.
 *
 * Clean Architecture data 계층 구현체
 * domain 계층 인터페이스 구현
 *
 * @see AutoRunSettingsRepository
 */
class AutoRunSettingsRepositoryImpl(private val context: Context) : AutoRunSettingsRepository {

    companion object {
        private const val TAG = "AutoRunSettingsRepositoryImpl"
        private const val PREFS_NAME = "auto_run_settings"

        // DataStore Keys
        private val KEY_EXCLUDE_WEEKENDS = booleanPreferencesKey("exclude_weekends")
        private val KEY_AUTO_START_DELAY_MINUTES = intPreferencesKey("auto_start_delay_minutes")
        private val KEY_PRE_NOTIFICATION_MINUTES = intPreferencesKey("pre_notification_minutes")

        // 기본값
        private const val DEFAULT_EXCLUDE_WEEKENDS = false
        private const val DEFAULT_AUTO_START_DELAY_MINUTES = 0
        private const val DEFAULT_PRE_NOTIFICATION_MINUTES = 5
    }

    // DataStore 인스턴스
    private val Context.dataStore by preferencesDataStore(name = PREFS_NAME)
    private val dataStore = context.dataStore

    // ==================== 조회 메서드 ====================

    /**
     * 주말 제외 설정 Flow
     */
    override val excludeWeekendsFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_EXCLUDE_WEEKENDS] ?: DEFAULT_EXCLUDE_WEEKENDS
    }

    /**
     * 자동 시작 딜레이 Flow (분)
     */
    override val autoStartDelayMinutesFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_AUTO_START_DELAY_MINUTES] ?: DEFAULT_AUTO_START_DELAY_MINUTES
    }

    /**
     * 사전 알림 시간 Flow (분)
     */
    override val preNotificationMinutesFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_PRE_NOTIFICATION_MINUTES] ?: DEFAULT_PRE_NOTIFICATION_MINUTES
    }

    // ==================== 저장 메서드 ====================

    /**
     * 주말 제외 설정 저장
     */
    override suspend fun saveExcludeWeekends(exclude: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_EXCLUDE_WEEKENDS] = exclude
        }
        Log.d(TAG, "Exclude weekends saved: $exclude")
    }

    /**
     * 자동 시작 딜레이 저장
     */
    override suspend fun saveAutoStartDelayMinutes(minutes: Int) {
        require(minutes in listOf(0, 5, 10)) { "Auto start delay must be 0, 5, or 10 minutes" }
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_START_DELAY_MINUTES] = minutes
        }
        Log.d(TAG, "Auto start delay saved: $minutes minutes")
    }

    /**
     * 사전 알림 시간 저장
     */
    override suspend fun savePreNotificationMinutes(minutes: Int) {
        require(minutes in listOf(0, 5, 10, 15)) { "Pre-notification must be 0, 5, 10, or 15 minutes" }
        dataStore.edit { preferences ->
            preferences[KEY_PRE_NOTIFICATION_MINUTES] = minutes
        }
        Log.d(TAG, "Pre-notification minutes saved: $minutes minutes")
    }

    // ==================== 동기 조회 메서드 ====================

    /**
     * 현재 주말 제외 설정 가져오기 (suspend)
     */
    override suspend fun getExcludeWeekends(): Boolean {
        return excludeWeekendsFlow.first()
    }

    /**
     * 현재 자동 시작 딜레이 가져오기 (suspend)
     */
    override suspend fun getAutoStartDelayMinutes(): Int {
        return autoStartDelayMinutesFlow.first()
    }

    /**
     * 현재 사전 알림 시간 가져오기 (suspend)
     */
    override suspend fun getPreNotificationMinutes(): Int {
        return preNotificationMinutesFlow.first()
    }
}

