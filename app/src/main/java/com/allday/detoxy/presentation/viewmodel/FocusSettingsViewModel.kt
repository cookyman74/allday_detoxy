package com.allday.detoxy.presentation.viewmodel

import android.app.Application
import android.app.NotificationManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.core.manager.DndManager
import com.allday.detoxy.core.utils.AnalyticsHelper
import com.allday.detoxy.core.utils.AppCategory
import com.allday.detoxy.core.utils.AppCategoryMapper
import com.allday.detoxy.core.utils.PermissionUtils
import com.allday.detoxy.domain.repository.FocusSettingsRepository
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
    application: Application,
    private val settingsRepository: FocusSettingsRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "FocusSettingsViewModel"
    }

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
     * Repository에서 설정 로드
     */
    private fun loadSettings() {
        viewModelScope.launch {
            // 각 설정을 Flow로 수집
            launch {
                settingsRepository.enabledCategoriesFlow.collect { categories ->
                    val detectedPreset = AppCategoryMapper.detectPreset(categories, _uiState.value.otherAppsEnabled)
                    _uiState.update { it.copy(enabledCategories = categories, selectedPreset = detectedPreset) }
                }
            }
            launch {
                settingsRepository.otherAppsEnabledFlow.collect { otherApps ->
                    val detectedPreset = AppCategoryMapper.detectPreset(_uiState.value.enabledCategories, otherApps)
                    _uiState.update { it.copy(otherAppsEnabled = otherApps, selectedPreset = detectedPreset) }
                }
            }
            launch {
                settingsRepository.messengerHasBeenEnabledFlow.collect { messengerHasBeenEnabled ->
                    _uiState.update { it.copy(messengerHasBeenEnabled = messengerHasBeenEnabled) }
                }
            }
            launch {
                settingsRepository.routineEnabledFlow.collect { routineEnabled ->
                    _uiState.update { it.copy(routineEnabled = routineEnabled) }
                }
            }
            // 흑백 모드 설정 수집
            launch {
                settingsRepository.grayscaleModeEnabledFlow.collect { enabled ->
                    _uiState.update { it.copy(grayscaleModeEnabled = enabled) }
                }
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

            // Repository에 저장
            settingsRepository.saveEnabledCategories(state.enabledCategories)
            settingsRepository.saveOtherAppsEnabled(state.otherAppsEnabled)
            settingsRepository.saveMessengerHasBeenEnabled(state.messengerHasBeenEnabled)
            settingsRepository.saveRoutineEnabled(state.routineEnabled)

            // AccessibilityService에 설정 전달
            FocusAccessibilityService.updateBlockSettings(
                state.enabledCategories,
                state.otherAppsEnabled
            )

            // Analytics 이벤트 로깅 (detoxy_settings_saved)
            val presetName = when (state.selectedPreset) {
                AppCategoryMapper.DetoxyPreset.COMPLETE_BLOCK -> "complete_block"
                AppCategoryMapper.DetoxyPreset.STANDARD_DETOXY -> "standard"
                AppCategoryMapper.DetoxyPreset.RELAXED -> "relaxed"
                null -> "custom"
            }
            AnalyticsHelper.logDetoxySettingsSaved(
                preset = presetName,
                enabledCategories = state.enabledCategories,
                otherAppsEnabled = state.otherAppsEnabled
            )

            Log.i(TAG, "Settings saved: ${state.enabledCategories.size} categories, other=${state.otherAppsEnabled}")
        }
    }

    /**
     * 흑백 모드 토글
     * 
     * Android 15+ 에서만 실제 흑백 전환 동작
     * 권한 체크 후 미승인 시 다이얼로그 표시
     * 
     * ⚠️ UI 정책: uiState.grayscaleModeEnabled는 "실제 활성화 상태"만 반영
     * - 권한 미승인 시 grayscaleModeEnabled는 false 유지 (낙관적 업데이트 안 함)
     * - 다이얼로그 닫힘 후 onGrayscalePermissionResult()에서 최종 상태 결정
     */
    fun toggleGrayscaleMode(enabled: Boolean) {
        // ⚠️ 토글 OFF 시: 다이얼로그가 열려있다면 닫기 (리뷰 피드백 반영)
        if (!enabled) {
            _uiState.update { 
                it.copy(
                    grayscaleModeEnabled = false, 
                    showGrayscalePermissionDialog = false  // 다이얼로그도 닫기
                ) 
            }
            saveGrayscaleSetting(false)
            Log.d(TAG, "Grayscale mode toggled OFF")
            return
        }
        
        // 토글 ON 시: API 레벨 게이트 확인
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // Android 14 이하: 설정은 저장하되 경고 배너 표시 (자동 전환 불가)
            _uiState.update { it.copy(grayscaleModeEnabled = true) }
            saveGrayscaleSetting(true)
            Log.d(TAG, "Grayscale mode toggled ON (Android 14-)")
            return
        }
        
        // Android 15+: 권한 확인
        val nm = getApplication<Application>()
            .getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) {
            // ⚠️ 권한 미승인: grayscaleModeEnabled를 명시적으로 false 설정
            // (저장값이 true인 상태에서 권한이 철회된 경우에도 false로 강제 동기화)
            _uiState.update { 
                it.copy(
                    grayscaleModeEnabled = false,  // 명시적 false (리뷰 피드백 반영)
                    showGrayscalePermissionDialog = true
                ) 
            }
            // 저장값도 false로 동기화 (권한 없으면 기능 사용 불가)
            saveGrayscaleSetting(false)
            Log.d(TAG, "Grayscale permission not granted, mode forced to OFF, showing dialog")
            return
        }
        
        // 권한 있음: 정상 토글 ON
        _uiState.update { it.copy(grayscaleModeEnabled = true) }
        saveGrayscaleSetting(true)
        Log.d(TAG, "Grayscale mode toggled ON (permission granted)")
    }

    /**
     * 흑백 모드 권한 요청 결과 처리
     */
    fun onGrayscalePermissionResult(granted: Boolean) {
        if (granted) {
            _uiState.update { it.copy(grayscaleModeEnabled = true) }
            saveGrayscaleSetting(true)
            Log.d(TAG, "Grayscale permission granted, mode enabled")
        } else {
            Log.d(TAG, "Grayscale permission denied, mode stays OFF")
        }
        // 다이얼로그 닫기
        _uiState.update { it.copy(showGrayscalePermissionDialog = false) }
    }

    /**
     * 흑백 모드 설정 저장
     */
    private fun saveGrayscaleSetting(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveGrayscaleModeEnabled(enabled)
        }
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
    val routineEnabled: Boolean = false,

    // 흑백 모드 설정 (v1.1)
    val grayscaleModeEnabled: Boolean = false,
    val showGrayscalePermissionDialog: Boolean = false
)