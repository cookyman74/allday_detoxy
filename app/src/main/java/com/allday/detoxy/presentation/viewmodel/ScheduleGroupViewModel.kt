package com.allday.detoxy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.data.local.entity.ScheduleGroup
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.domain.repository.ScheduleGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ScheduleGroup 화면 ViewModel
 *
 * ScheduleGroup CRUD 작업 및 연결된 자동 실행 설정 관리를 담당합니다.
 *
 * ## 주요 기능
 * - ScheduleGroup 목록 조회
 * - ScheduleGroup 생성/수정/삭제
 * - ScheduleGroup 활성화/비활성화
 * - 연결된 TimeBasedAutoRun 및 LocationBasedAutoRun 조회
 *
 * @param repository ScheduleGroupRepository
 */
@HiltViewModel
class ScheduleGroupViewModel @Inject constructor(
    private val repository: ScheduleGroupRepository
) : ViewModel() {

    /**
     * 모든 ScheduleGroup 목록 (Flow)
     */
    val scheduleGroups: StateFlow<List<ScheduleGroup>> = repository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * 에러 상태
     */
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    /**
     * 로딩 상태
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 선택된 ScheduleGroup의 연결된 TimeBasedAutoRun 목록
     */
    private val _linkedTimeBasedAutoRuns = MutableStateFlow<List<TimeBasedAutoRun>>(emptyList())
    val linkedTimeBasedAutoRuns: StateFlow<List<TimeBasedAutoRun>> = _linkedTimeBasedAutoRuns.asStateFlow()

    /**
     * 선택된 ScheduleGroup의 연결된 LocationBasedAutoRun 목록
     */
    private val _linkedLocations = MutableStateFlow<List<LocationBasedAutoRun>>(emptyList())
    val linkedLocations: StateFlow<List<LocationBasedAutoRun>> = _linkedLocations.asStateFlow()

    /**
     * ScheduleGroup 생성
     *
     * @param name 그룹 이름
     * @param description 그룹 설명 (옵션)
     */
    fun createScheduleGroup(name: String, description: String?) {
        if (name.isBlank()) {
            _errorState.value = "그룹 이름을 입력해주세요"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val scheduleGroup = ScheduleGroup(
                    name = name.trim(),
                    description = description?.trim()?.takeIf { it.isNotEmpty() }
                )
                repository.insert(scheduleGroup)
            } catch (e: Exception) {
                _errorState.value = "그룹 생성 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ScheduleGroup 수정
     *
     * @param scheduleGroup 수정할 ScheduleGroup
     */
    fun updateScheduleGroup(scheduleGroup: ScheduleGroup) {
        if (scheduleGroup.name.isBlank()) {
            _errorState.value = "그룹 이름을 입력해주세요"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.update(scheduleGroup)
            } catch (e: Exception) {
                _errorState.value = "그룹 수정 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ScheduleGroup 삭제
     *
     * 연결된 자동 실행 설정의 참조는 해제되지만, 자동 실행 설정 자체는 삭제되지 않습니다.
     *
     * @param scheduleGroupId 삭제할 ScheduleGroup ID
     */
    fun deleteScheduleGroup(scheduleGroupId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.delete(scheduleGroupId)
            } catch (e: Exception) {
                _errorState.value = "그룹 삭제 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ScheduleGroup 활성화/비활성화 토글
     *
     * @param scheduleGroupId ScheduleGroup ID
     * @param isActive 활성화 여부
     */
    fun toggleScheduleGroup(scheduleGroupId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleActive(scheduleGroupId, isActive)
            } catch (e: Exception) {
                _errorState.value = "그룹 상태 변경 실패: ${e.message}"
            }
        }
    }

    /**
     * 특정 ScheduleGroup의 연결된 TimeBasedAutoRun 목록 로드
     *
     * @param scheduleGroupId ScheduleGroup ID
     */
    fun loadLinkedTimeBasedAutoRuns(scheduleGroupId: String) {
        viewModelScope.launch {
            try {
                val linkedAutoRuns = repository.getLinkedTimeBasedAutoRuns(scheduleGroupId)
                _linkedTimeBasedAutoRuns.value = linkedAutoRuns
            } catch (e: Exception) {
                _errorState.value = "연결된 시간표 조회 실패: ${e.message}"
            }
        }
    }

    /**
     * 특정 ScheduleGroup의 연결된 LocationBasedAutoRun 목록 로드
     *
     * @param scheduleGroupId ScheduleGroup ID
     */
    fun loadLinkedLocations(scheduleGroupId: String) {
        viewModelScope.launch {
            try {
                val linkedLocs = repository.getLinkedLocations(scheduleGroupId)
                _linkedLocations.value = linkedLocs
            } catch (e: Exception) {
                _errorState.value = "연결된 위치 조회 실패: ${e.message}"
            }
        }
    }

    /**
     * 에러 상태 초기화
     */
    fun clearError() {
        _errorState.value = null
    }
}

