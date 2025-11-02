package com.allday.detoxy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.core.manager.ScheduleGroupManager
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.data.local.entity.ScheduleGroup
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.data.repository.TimeBasedAutoRunRepository
import com.allday.detoxy.domain.model.ScheduleTemplate
import com.allday.detoxy.domain.model.TimeSlot
import com.allday.detoxy.domain.repository.ScheduleGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * ScheduleGroup 화면 ViewModel
 *
 * ScheduleGroup CRUD 작업 및 연결된 자동 실행 설정 관리를 담당합니다.
 *
 * ## 주요 기능
 * - ScheduleGroup 목록 조회
 * - ScheduleGroup 생성/수정/삭제
 * - ScheduleGroup 활성화/비활성화 (ScheduleGroupManager 사용)
 * - 연결된 TimeBasedAutoRun 및 LocationBasedAutoRun 조회
 *
 * @param repository ScheduleGroupRepository
 * @param scheduleManager ScheduleGroupManager (3차 고도화: 시간표 활성화/비활성화)
 * @param timeBasedRepository TimeBasedAutoRunRepository (3.5차 고도화 Phase 1: 시간대 생성)
 */
@HiltViewModel
class ScheduleGroupViewModel @Inject constructor(
    private val repository: ScheduleGroupRepository,
    private val scheduleManager: ScheduleGroupManager,
    private val timeBasedRepository: TimeBasedAutoRunRepository
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
     * 활성화된 ScheduleGroup (Flow)
     *
     * 3차 고도화: 한 번에 하나의 시간표만 활성화 가능 (단일 활성화 원칙)
     */
    val activeGroup: StateFlow<ScheduleGroup?> = repository.getActive()
        .map { it.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
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
     * 각 ScheduleGroup의 연결된 TimeBasedAutoRun 개수 (Map 기반 캐싱)
     * 
     * Key: ScheduleGroup ID
     * Value: 연결된 TimeBasedAutoRun 개수
     */
    private val _linkedTimeBasedAutoRunCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val linkedTimeBasedAutoRunCounts: StateFlow<Map<String, Int>> = _linkedTimeBasedAutoRunCounts.asStateFlow()

    /**
     * 각 ScheduleGroup의 연결된 LocationBasedAutoRun 개수 (Map 기반 캐싱)
     * 
     * Key: ScheduleGroup ID
     * Value: 연결된 LocationBasedAutoRun 개수
     */
    private val _linkedLocationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val linkedLocationCounts: StateFlow<Map<String, Int>> = _linkedLocationCounts.asStateFlow()

    /**
     * ScheduleGroup 생성
     *
     * @param name 그룹 이름
     * @param description 그룹 설명 (옵션)
     * @return 생성된 ScheduleGroup의 ID (3차 고도화 개선: 빠른 시간표 생성 시 자동 선택용)
     */
    suspend fun createScheduleGroup(name: String, description: String?): String {
        if (name.isBlank()) {
            _errorState.value = "그룹 이름을 입력해주세요"
            throw IllegalArgumentException("그룹 이름을 입력해주세요")
        }

        try {
            _isLoading.value = true
            val scheduleGroup = ScheduleGroup(
                name = name.trim(),
                description = description?.trim()?.takeIf { it.isNotEmpty() }
            )
            repository.insert(scheduleGroup)
            return scheduleGroup.id
        } catch (e: Exception) {
            _errorState.value = "그룹 생성 실패: ${e.message}"
            throw e
        } finally {
            _isLoading.value = false
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
     * 2.5차 고도화: 단순 toggle (UI에서 직접 사용 시)
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
     * ScheduleGroup 활성화
     *
     * 3차 고도화: ScheduleGroupManager를 통한 시간표 활성화
     * - 다른 모든 그룹 자동 비활성화 (단일 활성화 원칙)
     * - 그룹 내 활성화된 시간대의 알람 자동 등록
     *
     * @param scheduleGroupId ScheduleGroup ID
     */
    fun activateGroup(scheduleGroupId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = scheduleManager.activateGroup(scheduleGroupId)
                if (result.isFailure) {
                    _errorState.value = "시간표 활성화 실패: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorState.value = "시간표 활성화 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ScheduleGroup 비활성화
     *
     * 3차 고도화: ScheduleGroupManager를 통한 시간표 비활성화
     * - 그룹 내 모든 시간대의 알람 자동 취소
     *
     * @param scheduleGroupId ScheduleGroup ID
     */
    fun deactivateGroup(scheduleGroupId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = scheduleManager.deactivateGroup(scheduleGroupId)
                if (result.isFailure) {
                    _errorState.value = "시간표 비활성화 실패: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorState.value = "시간표 비활성화 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 각 ScheduleGroup의 연결된 TimeBasedAutoRun 목록 (Map 기반 캐싱)
     * 
     * Key: ScheduleGroup ID
     * Value: 연결된 TimeBasedAutoRun 목록
     */
    private val _linkedTimeBasedAutoRuns = MutableStateFlow<Map<String, List<TimeBasedAutoRun>>>(emptyMap())
    val linkedTimeBasedAutoRuns: StateFlow<Map<String, List<TimeBasedAutoRun>>> = _linkedTimeBasedAutoRuns.asStateFlow()

    /**
     * 특정 ScheduleGroup의 연결된 TimeBasedAutoRun 개수 로드
     *
     * @param scheduleGroupId ScheduleGroup ID
     */
    fun loadLinkedTimeBasedAutoRunCount(scheduleGroupId: String) {
        viewModelScope.launch {
            try {
                val count = repository.getLinkedTimeBasedAutoRunCount(scheduleGroupId)
                _linkedTimeBasedAutoRunCounts.value = _linkedTimeBasedAutoRunCounts.value + (scheduleGroupId to count)
            } catch (e: Exception) {
                _errorState.value = "연결된 시간표 조회 실패: ${e.message}"
            }
        }
    }

    /**
     * 특정 ScheduleGroup의 연결된 LocationBasedAutoRun 개수 로드
     *
     * @param scheduleGroupId ScheduleGroup ID
     */
    fun loadLinkedLocationCount(scheduleGroupId: String) {
        viewModelScope.launch {
            try {
                val count = repository.getLinkedLocationCount(scheduleGroupId)
                _linkedLocationCounts.value = _linkedLocationCounts.value + (scheduleGroupId to count)
            } catch (e: Exception) {
                _errorState.value = "연결된 위치 조회 실패: ${e.message}"
            }
        }
    }

    /**
     * 모든 ScheduleGroup의 연결된 설정 개수 및 시간대 목록 로드
     * 
     * 화면 초기 로딩 시 호출하여 모든 그룹의 데이터를 한 번에 로드합니다.
     * 
     * ## 3차 고도화 개선
     * - 시간대 목록도 함께 로드하여 Card에서 ViewModel 호출 불필요
     * - 성능 개선: 불필요한 Flow 재생성 방지
     */
    fun loadAllLinkedCounts() {
        viewModelScope.launch {
            try {
                val groups = scheduleGroups.value
                val timeCounts = mutableMapOf<String, Int>()
                val locationCounts = mutableMapOf<String, Int>()
                val timeBasedAutoRuns = mutableMapOf<String, List<TimeBasedAutoRun>>()
                
                groups.forEach { group ->
                    // 시간대 목록 로드
                    val linkedAutoRuns = repository.getLinkedTimeBasedAutoRuns(group.id)
                    timeBasedAutoRuns[group.id] = linkedAutoRuns
                    timeCounts[group.id] = linkedAutoRuns.size
                    
                    // 위치 개수 로드
                    locationCounts[group.id] = repository.getLinkedLocationCount(group.id)
                }
                
                _linkedTimeBasedAutoRuns.value = timeBasedAutoRuns
                _linkedTimeBasedAutoRunCounts.value = timeCounts
                _linkedLocationCounts.value = locationCounts
            } catch (e: Exception) {
                _errorState.value = "연결된 설정 조회 실패: ${e.message}"
            }
        }
    }

    /**
     * 시간대를 포함한 시간표 생성 (Phase 1)
     *
     * QuickCreateScheduleDialog의 커스텀 모드에서 사용됩니다.
     * TimeSlot 목록을 받아 ScheduleGroup과 TimeBasedAutoRun들을 생성합니다.
     *
     * ## 동작 방식
     * 1. ScheduleGroup 생성
     * 2. 각 TimeSlot에 대해 TimeBasedAutoRun 생성
     * 3. TimeBasedAutoRun을 ScheduleGroup에 연결 (scheduleGroupId 설정)
     *
     * @param name 시간표 이름
     * @param description 시간표 설명 (옵션)
     * @param timeSlots 시간대 목록
     * @return 생성된 ScheduleGroup의 ID
     * @throws IllegalArgumentException 이름이 비어있거나 시간대가 없을 때
     */
    suspend fun createScheduleGroupWithTimeSlots(
        name: String,
        description: String?,
        timeSlots: List<TimeSlot>
    ): String {
        if (name.isBlank()) {
            _errorState.value = "그룹 이름을 입력해주세요"
            throw IllegalArgumentException("그룹 이름을 입력해주세요")
        }
        
        if (timeSlots.isEmpty()) {
            _errorState.value = "최소 1개 이상의 시간대를 추가해주세요"
            throw IllegalArgumentException("최소 1개 이상의 시간대를 추가해주세요")
        }
        
        try {
            _isLoading.value = true
            
            // 1. ScheduleGroup 생성
            val scheduleGroup = ScheduleGroup(
                name = name.trim(),
                description = description?.trim()?.takeIf { it.isNotEmpty() }
            )
            repository.insert(scheduleGroup)
            
            // 2. TimeBasedAutoRun 생성 (각 시간대마다)
            timeSlots.forEach { slot ->
                val autoRun = TimeBasedAutoRun(
                    hour = slot.startHour,
                    minute = slot.startMinute,
                    durationMinutes = slot.durationMinutes,
                    presetType = slot.presetType,
                    enabledDays = formatEnabledDays(slot.enabledDays),  // List<DayOfWeek> → JSON 문자열
                    scheduleGroupId = scheduleGroup.id,  // 그룹 연결
                    isIndependent = false,  // 그룹에 종속
                    isEnabled = true,
                    createdAt = System.currentTimeMillis()
                )
                timeBasedRepository.insert(autoRun)
            }
            
            return scheduleGroup.id
        } catch (e: Exception) {
            _errorState.value = "그룹 생성 실패: ${e.message}"
            throw e
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 템플릿으로 시간표 생성 (Phase 2)
     *
     * ScheduleTemplate을 기반으로 ScheduleGroup과 TimeBasedAutoRun들을 생성합니다.
     * createScheduleGroupWithTimeSlots를 재사용하여 구현합니다.
     *
     * ## 동작 방식
     * 1. 템플릿의 timeSlots를 사용하여 createScheduleGroupWithTimeSlots 호출
     * 2. 템플릿의 description을 시간표 설명으로 사용
     *
     * @param name 시간표 이름 (사용자 입력)
     * @param template 선택한 템플릿
     * @return 생성된 ScheduleGroup의 ID
     * @throws IllegalArgumentException 이름이 비어있거나 템플릿에 시간대가 없을 때
     */
    suspend fun createFromTemplate(
        name: String,
        template: ScheduleTemplate
    ): String {
        return createScheduleGroupWithTimeSlots(
            name = name,
            description = template.description,
            timeSlots = template.timeSlots
        )
    }
    
    /**
     * List<DayOfWeek>를 JSON 문자열로 변환
     *
     * @param days 요일 목록
     * @return JSON 문자열 (예: "[\"MON\",\"TUE\",\"WED\",\"THU\",\"FRI\"]")
     */
    private fun formatEnabledDays(days: List<DayOfWeek>): String {
        val dayStrings = days.map { day ->
            when (day) {
                DayOfWeek.MONDAY -> "MON"
                DayOfWeek.TUESDAY -> "TUE"
                DayOfWeek.WEDNESDAY -> "WED"
                DayOfWeek.THURSDAY -> "THU"
                DayOfWeek.FRIDAY -> "FRI"
                DayOfWeek.SATURDAY -> "SAT"
                DayOfWeek.SUNDAY -> "SUN"
            }
        }
        return "[${dayStrings.joinToString(",") { "\"$it\"" }}]"
    }

    /**
     * 에러 상태 초기화
     */
    fun clearError() {
        _errorState.value = null
    }
}

