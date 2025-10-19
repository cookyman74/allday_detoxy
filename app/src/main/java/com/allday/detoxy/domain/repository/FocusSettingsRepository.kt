package com.allday.detoxy.domain.repository

import com.allday.detoxy.core.utils.AppCategory
import com.allday.detoxy.core.utils.AppCategoryMapper
import kotlinx.coroutines.flow.Flow

/**
 * 디톡시 제어 설정 Repository 인터페이스
 *
 * Clean Architecture의 domain 계층 인터페이스
 * 구현체는 data 계층에 위치 (FocusSettingsRepositoryImpl)
 *
 * 주요 기능:
 * 1. 카테고리별 차단 설정 저장/로드
 * 2. 프리셋 적용
 * 3. 메신저 카테고리 첫 활성화 플래그
 * 4. 디톡시 루틴 설정 (향후 확장)
 *
 * @see com.allday.detoxy.data.repository.FocusSettingsRepositoryImpl
 * @see com.allday.detoxy.presentation.viewmodel.FocusSettingsViewModel
 * @see com.allday.detoxy.presentation.viewmodel.TimerViewModel
 */
interface FocusSettingsRepository {

    // ==================== Flow (실시간 관찰) ====================

    /**
     * 활성화된 카테고리 목록 Flow
     */
    val enabledCategoriesFlow: Flow<Set<AppCategory>>

    /**
     * 기타 앱 차단 여부 Flow
     */
    val otherAppsEnabledFlow: Flow<Boolean>

    /**
     * 메신저 카테고리 활성화 이력 Flow
     */
    val messengerHasBeenEnabledFlow: Flow<Boolean>

    /**
     * 디톡시 루틴 활성화 여부 Flow
     */
    val routineEnabledFlow: Flow<Boolean>

    /**
     * 현재 프리셋 감지 Flow
     */
    val currentPresetFlow: Flow<AppCategoryMapper.DetoxyPreset?>

    // ==================== 저장 메서드 ====================

    /**
     * 활성화된 카테고리 목록 저장
     */
    suspend fun saveEnabledCategories(categories: Set<AppCategory>)

    /**
     * 기타 앱 차단 여부 저장
     */
    suspend fun saveOtherAppsEnabled(enabled: Boolean)

    /**
     * 메신저 카테고리 활성화 이력 저장
     */
    suspend fun saveMessengerHasBeenEnabled(enabled: Boolean)

    /**
     * 디톡시 루틴 활성화 여부 저장
     */
    suspend fun saveRoutineEnabled(enabled: Boolean)

    /**
     * 프리셋 적용
     */
    suspend fun applyPreset(preset: AppCategoryMapper.DetoxyPreset)

    // ==================== 동기 조회 (suspend) ====================

    /**
     * 현재 활성화된 카테고리 가져오기 (suspend)
     */
    suspend fun getEnabledCategories(): Set<AppCategory>

    /**
     * 현재 기타 앱 차단 여부 가져오기 (suspend)
     */
    suspend fun getOtherAppsEnabled(): Boolean

    /**
     * 현재 설정 가져오기 (카테고리 + 기타 앱)
     */
    suspend fun getCurrentSettings(): Pair<Set<AppCategory>, Boolean>
}
