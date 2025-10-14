package com.allday.detoxy.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.core.manager.DndManager
import com.allday.detoxy.core.utils.AppCategory
import com.allday.detoxy.core.utils.AppCategoryMapper
import com.allday.detoxy.core.utils.PermissionUtils
import com.allday.detoxy.service.accessibility.FocusAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 디톡시 제어 설정 화면 ViewModel
 *
 * 주요 기능:
 * 1. 카테고리별 차단 설정 관리
 * 2. 프리셋 적용 및 감지
 * 3. 권한 상태 모니터링
 * 4. DataStore 영속화
 *
 * @see DetoxyControlSettingsScreen
 */
@HiltViewModel
class FocusSettingsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "FocusSettingsViewModel"
        private const val PREFS_NAME = "focus_settings"

        // DataStore Keys
        private val KEY_ENABLED_CATEGORIES = stringSetPreferencesKey("enabled_categories")
        private val KEY_OTHER_APPS_ENABLED = booleanPreferencesKey("other_apps_enabled")
        private val KEY_MESSENGER_HAS_BEEN_ENABLED = booleanPreferencesKey("messenger_has_been_enabled")
        private val KEY_ROUTINE_ENABLED = booleanPreferencesKey("routine_enabled")
    }

    // DataStore 인스턴스
    private val Context.dataStore by preferencesDataStore(name = PREFS_NAME)
    private val dataStore = application.dataStore

    // DND Manager
    private val dndManager = DndManager(application)

    // UI State
    private val _uiState = MutableStateFlow(FocusSettingsUiState())
    val uiState: StateFlow<FocusSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        monitorPermissions()
    }

    /**
     * DataStore에서 설정 로드
     */
    private fun loadSettings() {
        viewModelScope.launch {
            dataStore.data.collect { preferences ->
                val categories = preferences[KEY_ENABLED_CATEGORIES]?.mapNotNull { name ->
                    try {
                        AppCategory.valueOf(name)
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "Unknown category: $name")
                        null
                    }
                }?.toSet() ?: getDefaultEnabledCategories()

                val otherAppsEnabled = preferences[KEY_OTHER_APPS_ENABLED] ?: false
                val messengerHasBeenEnabled = preferences[KEY_MESSENGER_HAS_BEEN_ENABLED] ?: false
                val routineEnabled = preferences[KEY_ROUTINE_ENABLED] ?: false

                // 프리셋 감지
                val detectedPreset = AppCategoryMapper.detectPreset(categories, otherAppsEnabled)

                _uiState.update { state ->
                    state.copy(
                        enabledCategories = categories,
                        otherAppsEnabled = otherAppsEnabled,
                        messengerHasBeenEnabled = messengerHasBeenEnabled,
                        routineEnabled = routineEnabled,
                        selectedPreset = detectedPreset
                    )
                }

                Log.d(TAG, "Settings loaded: ${categories.size} categories, preset: ${detectedPreset?.displayName}")
            }
        }
    }

    /**
     * 권한 상태 모니터링
     */
    private fun monitorPermissions() {
        val context = getApplication<Application>()

        // 초기 권한 상태 확인
        updatePermissionStates()

        // 주기적으로 권한 상태 체크 (화면이 다시 표시될 때 업데이트)
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    dndPermissionState = dndManager.getPermissionState(),
                    accessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(context),
                    overlayEnabled = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        Settings.canDrawOverlays(context)
                    } else true
                )
            }
        }
    }

    /**
     * 권한 상태 업데이트
     */
    fun updatePermissionStates() {
        val context = getApplication<Application>()
        _uiState.update { state ->
            state.copy(
                dndPermissionState = dndManager.getPermissionState(),
                accessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(context),
                overlayEnabled = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else true
            )
        }
    }

    /**
     * 프리셋 적용
     */
    fun applyPreset(preset: AppCategoryMapper.DetoxyPreset) {
        val (categories, otherAppsEnabled) = AppCategoryMapper.applyPreset(preset)

        _uiState.update { state ->
            state.copy(
                enabledCategories = categories,
                otherAppsEnabled = otherAppsEnabled,
                selectedPreset = preset
            )
        }

        Log.i(TAG, "Preset applied: ${preset.displayName}")
    }

    /**
     * 카테고리 토글
     */
    fun toggleCategory(category: AppCategory, enabled: Boolean) {
        _uiState.update { state ->
            val updatedCategories = if (enabled) {
                state.enabledCategories + category
            } else {
                state.enabledCategories - category
            }

            // 프리셋 재감지
            val detectedPreset = AppCategoryMapper.detectPreset(updatedCategories, state.otherAppsEnabled)

            state.copy(
                enabledCategories = updatedCategories,
                selectedPreset = detectedPreset
            )
        }

        Log.d(TAG, "Category toggled: ${category.getDisplayName()} = $enabled")
    }

    /**
     * 기타 앱 토글
     */
    fun toggleOtherApps(enabled: Boolean) {
        _uiState.update { state ->
            // 프리셋 재감지
            val detectedPreset = AppCategoryMapper.detectPreset(state.enabledCategories, enabled)

            state.copy(
                otherAppsEnabled = enabled,
                selectedPreset = detectedPreset
            )
        }

        Log.d(TAG, "Other apps toggled: $enabled")
    }

    /**
     * 메신저 카테고리가 활성화된 적이 있음을 기록
     */
    fun setMessengerHasBeenEnabled() {
        _uiState.update { state ->
            state.copy(messengerHasBeenEnabled = true)
        }
    }

    /**
     * 디톡시 루틴 토글
     */
    fun toggleRoutine(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(routineEnabled = enabled)
        }

        Log.d(TAG, "Detoxy routine toggled: $enabled")
    }

    /**
     * 설정 저장
     */
    fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value

            dataStore.edit { preferences ->
                // 카테고리 저장 (enum name으로)
                preferences[KEY_ENABLED_CATEGORIES] = state.enabledCategories.map { it.name }.toSet()
                preferences[KEY_OTHER_APPS_ENABLED] = state.otherAppsEnabled
                preferences[KEY_MESSENGER_HAS_BEEN_ENABLED] = state.messengerHasBeenEnabled
                preferences[KEY_ROUTINE_ENABLED] = state.routineEnabled
            }

            // AccessibilityService에 설정 전달
            FocusAccessibilityService.updateBlockSettings(
                state.enabledCategories,
                state.otherAppsEnabled
            )

            Log.i(TAG, "Settings saved: ${state.enabledCategories.size} categories, other=${ state.otherAppsEnabled}")
        }
    }

    /**
     * 기본 활성화 카테고리 (표준 디톡시)
     */
    private fun getDefaultEnabledCategories(): Set<AppCategory> {
        return setOf(
            AppCategory.SNS,
            AppCategory.WEB,
            AppCategory.VIDEO_SHORTS
        )
    }
}

/**
 * 디톡시 제어 설정 화면 UI 상태
 */
data class FocusSettingsUiState(
    // 차단 설정
    val enabledCategories: Set<AppCategory> = setOf(
        AppCategory.SNS,
        AppCategory.WEB,
        AppCategory.VIDEO_SHORTS
    ),
    val otherAppsEnabled: Boolean = false,

    // 프리셋
    val selectedPreset: AppCategoryMapper.DetoxyPreset? = AppCategoryMapper.DetoxyPreset.STANDARD_DETOXY,

    // 메신저 안내 다이얼로그 표시 여부
    val messengerHasBeenEnabled: Boolean = false,

    // 권한 상태
    val dndPermissionState: DndManager.DndPermissionState = DndManager.DndPermissionState.DENIED,
    val accessibilityEnabled: Boolean = false,
    val overlayEnabled: Boolean = false,

    // 디톡시 루틴
    val routineEnabled: Boolean = false
)