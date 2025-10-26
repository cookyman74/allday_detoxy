package com.allday.detoxy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.data.local.entity.CustomTimerPreset
import com.allday.detoxy.data.repository.CustomTimerPresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * CustomTimerPreset ViewModel
 *
 * 커스텀 타이머 프리셋 UI 상태를 관리합니다.
 *
 * ## 주요 기능
 * - 프리셋 CRUD 작업
 * - 표시 순서 관리
 * - 사용 횟수 자동 증가
 *
 * @property repository CustomTimerPresetRepository
 */
@HiltViewModel
class CustomTimerPresetViewModel @Inject constructor(
    private val repository: CustomTimerPresetRepository
) : ViewModel() {

    /**
     * 모든 커스텀 프리셋 (표시 순서대로)
     */
    val presets: StateFlow<List<CustomTimerPreset>> = repository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * 새로운 프리셋 저장
     *
     * @param name 프리셋 이름
     * @param durationMinutes 타이머 시간 (분)
     * @param presetType 차단 프리셋 (옵션)
     */
    fun savePreset(name: String, durationMinutes: Int, presetType: String?) {
        viewModelScope.launch {
            // 다음 표시 순서 계산 (가장 마지막에 추가)
            val count = repository.getCount()
            
            val preset = CustomTimerPreset(
                name = name,
                durationMinutes = durationMinutes,
                presetType = presetType,
                usageCount = 0,
                displayOrder = count // 마지막 순서로 추가
            )
            
            repository.insert(preset)
        }
    }

    /**
     * 프리셋 업데이트
     *
     * @param preset 업데이트할 프리셋
     */
    fun updatePreset(preset: CustomTimerPreset) {
        viewModelScope.launch {
            repository.update(preset)
        }
    }

    /**
     * 프리셋 삭제
     *
     * @param presetId 삭제할 프리셋 ID
     */
    fun deletePreset(presetId: String) {
        viewModelScope.launch {
            repository.delete(presetId)
            
            // 삭제 후 표시 순서 재정렬
            reorderPresets()
        }
    }

    /**
     * 프리셋 표시 순서 업데이트
     *
     * @param presets 순서가 변경된 프리셋 리스트
     */
    fun updateDisplayOrder(presets: List<CustomTimerPreset>) {
        viewModelScope.launch {
            presets.forEachIndexed { index, preset ->
                repository.updateDisplayOrder(preset.id, index)
            }
        }
    }

    /**
     * 프리셋 사용 횟수 증가
     *
     * @param presetId 프리셋 ID
     */
    fun incrementUsageCount(presetId: String) {
        viewModelScope.launch {
            repository.incrementUsageCount(presetId)
        }
    }

    /**
     * 표시 순서 재정렬
     *
     * 삭제나 편집 후 순서가 어긋났을 때 호출합니다.
     */
    private suspend fun reorderPresets() {
        val currentPresets = repository.getAll().stateIn(viewModelScope).value
        currentPresets.forEachIndexed { index, preset ->
            repository.updateDisplayOrder(preset.id, index)
        }
    }
}

