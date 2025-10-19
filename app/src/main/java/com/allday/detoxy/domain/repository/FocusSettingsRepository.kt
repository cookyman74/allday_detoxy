package com.allday.detoxy.domain.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.allday.detoxy.core.utils.AppCategory
import com.allday.detoxy.core.utils.AppCategoryMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 디톡시 제어 설정 Repository
 *
 * DataStore를 통해 카테고리별 차단 설정을 관리합니다.
 *
 * 주요 기능:
 * 1. 카테고리별 차단 설정 저장/로드
 * 2. 프리셋 적용
 * 3. 메신저 카테고리 첫 활성화 플래그
 * 4. 디톡시 루틴 설정 (향후 확장)
 *
 * @see FocusSettingsViewModel
 * @see TimerViewModel
 */
class FocusSettingsRepository(private val context: Context) {

    companion object {
        private const val TAG = "FocusSettingsRepository"
        private const val PREFS_NAME = "focus_settings"

        // DataStore Keys
        private val KEY_ENABLED_CATEGORIES = stringSetPreferencesKey("enabled_categories")
        private val KEY_OTHER_APPS_ENABLED = booleanPreferencesKey("other_apps_enabled")
        private val KEY_MESSENGER_HAS_BEEN_ENABLED = booleanPreferencesKey("messenger_has_been_enabled")
        private val KEY_ROUTINE_ENABLED = booleanPreferencesKey("routine_enabled")
    }

    // DataStore 인스턴스
    private val Context.dataStore by preferencesDataStore(name = PREFS_NAME)
    private val dataStore = context.dataStore

    // ==================== 조회 메서드 ====================

    /**
     * 활성화된 카테고리 목록 Flow
     */
    val enabledCategoriesFlow: Flow<Set<AppCategory>> = dataStore.data.map { preferences ->
        preferences[KEY_ENABLED_CATEGORIES]?.mapNotNull { name ->
            try {
                AppCategory.valueOf(name)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Unknown category: $name")
                null
            }
        }?.toSet() ?: getDefaultEnabledCategories()
    }

    /**
     * 기타 앱 차단 여부 Flow
     */
    val otherAppsEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_OTHER_APPS_ENABLED] ?: false
    }

    /**
     * 메신저 카테고리 활성화 이력 Flow
     */
    val messengerHasBeenEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_MESSENGER_HAS_BEEN_ENABLED] ?: false
    }

    /**
     * 디톡시 루틴 활성화 여부 Flow
     */
    val routineEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ROUTINE_ENABLED] ?: false
    }

    /**
     * 현재 프리셋 감지 Flow
     */
    val currentPresetFlow: Flow<AppCategoryMapper.DetoxyPreset?> = dataStore.data.map { preferences ->
        val categories = preferences[KEY_ENABLED_CATEGORIES]?.mapNotNull { name ->
            try {
                AppCategory.valueOf(name)
            } catch (e: IllegalArgumentException) {
                null
            }
        }?.toSet() ?: getDefaultEnabledCategories()

        val otherApps = preferences[KEY_OTHER_APPS_ENABLED] ?: false

        AppCategoryMapper.detectPreset(categories, otherApps)
    }

    // ==================== 저장 메서드 ====================

    /**
     * 활성화된 카테고리 목록 저장
     */
    suspend fun saveEnabledCategories(categories: Set<AppCategory>) {
        dataStore.edit { preferences ->
            preferences[KEY_ENABLED_CATEGORIES] = categories.map { it.name }.toSet()
        }
        Log.d(TAG, "Enabled categories saved: ${categories.size} categories")
    }

    /**
     * 기타 앱 차단 여부 저장
     */
    suspend fun saveOtherAppsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_OTHER_APPS_ENABLED] = enabled
        }
        Log.d(TAG, "Other apps enabled saved: $enabled")
    }

    /**
     * 메신저 카테고리 활성화 이력 저장
     */
    suspend fun saveMessengerHasBeenEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_MESSENGER_HAS_BEEN_ENABLED] = enabled
        }
        Log.d(TAG, "Messenger has been enabled saved: $enabled")
    }

    /**
     * 디톡시 루틴 활성화 여부 저장
     */
    suspend fun saveRoutineEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ROUTINE_ENABLED] = enabled
        }
        Log.d(TAG, "Routine enabled saved: $enabled")
    }

    /**
     * 프리셋 적용
     */
    suspend fun applyPreset(preset: AppCategoryMapper.DetoxyPreset) {
        val (categories, otherApps) = AppCategoryMapper.applyPreset(preset)
        dataStore.edit { preferences ->
            preferences[KEY_ENABLED_CATEGORIES] = categories.map { it.name }.toSet()
            preferences[KEY_OTHER_APPS_ENABLED] = otherApps
        }
        Log.i(TAG, "Preset applied: ${preset.displayName}")
    }

    // ==================== 동기 조회 메서드 (suspend) ====================

    /**
     * 현재 활성화된 카테고리 가져오기 (suspend)
     */
    suspend fun getEnabledCategories(): Set<AppCategory> {
        return enabledCategoriesFlow.first()
    }

    /**
     * 현재 기타 앱 차단 여부 가져오기 (suspend)
     */
    suspend fun getOtherAppsEnabled(): Boolean {
        return otherAppsEnabledFlow.first()
    }

    /**
     * 현재 설정 가져오기 (카테고리 + 기타 앱)
     */
    suspend fun getCurrentSettings(): Pair<Set<AppCategory>, Boolean> {
        val categories = getEnabledCategories()
        val otherApps = getOtherAppsEnabled()
        return Pair(categories, otherApps)
    }

    // ==================== 기본값 ====================

    /**
     * 기본 활성화 카테고리 (표준 디톡시 프리셋)
     */
    private fun getDefaultEnabledCategories(): Set<AppCategory> {
        return setOf(
            AppCategory.SNS,
            AppCategory.WEB,
            AppCategory.VIDEO_SHORTS
        )
    }
}

