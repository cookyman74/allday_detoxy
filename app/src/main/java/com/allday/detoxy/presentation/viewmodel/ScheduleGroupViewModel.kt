package com.allday.detoxy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.core.manager.ScheduleGroupManager
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.data.local.entity.ScheduleGroup
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.data.repository.TimeBasedAutoRunRepository
import com.allday.detoxy.domain.model.PauseDuration
import com.allday.detoxy.domain.model.ScheduleGroupControlState
import com.allday.detoxy.domain.model.ScheduleTemplate
import com.allday.detoxy.domain.model.TimeSlot
import com.allday.detoxy.domain.repository.ScheduleGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
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

    init {
        // v8: 앱 시작 시 만료된 일시중지 상태 자동 해제
        clearExpiredPausesOnInit()
    }

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
     * 활성화된 ScheduleGroup 목록 (Flow)
     *
     * v0.10.1: 여러 스케줄 그룹이 동시에 활성화될 수 있음
     * - 위치 기반 스케줄: 각 위치마다 별도의 스케줄 활성화 가능
     * - 시간 기반 스케줄: 여러 시간표 동시 활성화 가능
     */
    val activeGroups: StateFlow<List<ScheduleGroup>> = repository.getActive()
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
     * 🆕 각 ScheduleGroup의 연결된 LocationBasedAutoRun 목록 (Map 기반 캐싱)
     * 
     * Key: ScheduleGroup ID
     * Value: 연결된 LocationBasedAutoRun 목록
     */
    private val _linkedLocations = MutableStateFlow<Map<String, List<com.allday.detoxy.data.local.entity.LocationBasedAutoRun>>>(emptyMap())
    val linkedLocations: StateFlow<Map<String, List<com.allday.detoxy.data.local.entity.LocationBasedAutoRun>>> = _linkedLocations.asStateFlow()

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
                // 사용자가 UI에서 비활성화 버튼을 클릭한 경우이므로 isUserAction=true
                // → manualOverrideState가 'INACTIVE'로 설정되어 자동 실행이 차단됨
                val result = scheduleManager.deactivateGroup(scheduleGroupId, isUserAction = true)
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

    // ==================== v8: 통합 제어 ====================

    /**
     * 스케줄 그룹 제어 상태 변경 (v8)
     *
     * ScheduleControlButton에서 호출되어 활성/비활성 상태를 변경합니다.
     *
     * ## 동작
     * - ACTIVE: manualOverrideState를 null로 설정 + activateGroup 호출
     * - INACTIVE: manualOverrideState를 "INACTIVE"로 설정 + deactivateGroup 호출
     * - PAUSED: pauseScheduleGroup() 메서드 사용
     *
     * @param groupId 스케줄 그룹 ID
     * @param newState 새로운 제어 상태
     */
    fun changeControlState(groupId: String, newState: ScheduleGroupControlState) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                when (newState) {
                    ScheduleGroupControlState.ACTIVE -> {
                        // 수동 활성화 (자동 모드 시작)
                        // v8 Fix: 무조건 활성화하지 않고, 위치 확인을 위해 Auto Mode 시작
                        // (위치 내부면 즉시 켜지고, 아니면 꺼진 상태로 대기)
                        scheduleManager.startAutoMode(groupId)
                    }
                    ScheduleGroupControlState.INACTIVE -> {
                        // 수동 비활성화 설정
                        // deactivateGroup(isUserAction=true) 내부에서 updateManualOverride 호출됨
                        // 하지만 명시성을 위해 repository 호출은 유지하거나, 중복이면 제거 가능
                        // 여기서는 Manager에 위임
                        scheduleManager.deactivateGroup(groupId, isUserAction = true)
                    }
                    ScheduleGroupControlState.PAUSED -> {
                        // PAUSED는 pauseScheduleGroup()으로 처리
                        // 여기서는 아무 동작 없음
                    }
                }
            } catch (e: Exception) {
                _errorState.value = "상태 변경 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 스케줄 그룹 일시중지 (v8)
     *
     * ScheduleControlButton 드롭다운에서 호출되어 일시중지를 설정합니다.
     * 일시중지 중에는 알람이 트리거되지 않지만 위치 감지는 유지됩니다.
     *
     * @param groupId 스케줄 그룹 ID
     * @param duration 일시중지 기간
     */
    fun pauseScheduleGroup(groupId: String, duration: PauseDuration) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val pauseUntil = duration.calculatePauseUntil()
                repository.updateManualOverride(groupId, "PAUSED", pauseUntil)
            } catch (e: Exception) {
                _errorState.value = "일시중지 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 만료된 일시중지 자동 해제 (v8)
     *
     * ViewModel 초기화 시 호출되어 만료된 일시중지 상태를 정리합니다.
     * 백그라운드에서 조용히 실행되며 UI에 영향을 주지 않습니다.
     */
    private fun clearExpiredPausesOnInit() {
        viewModelScope.launch {
            try {
                val clearedCount = repository.clearExpiredPauses()
                if (clearedCount > 0) {
                    android.util.Log.d("ScheduleGroupVM", "v8: Cleared $clearedCount expired pauses")
                }
            } catch (e: Exception) {
                // 초기화 실패 시 로그만 기록 (UI 에러 표시 안함)
                android.util.Log.w("ScheduleGroupVM", "Failed to clear expired pauses: ${e.message}")
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
                val locations = mutableMapOf<String, List<com.allday.detoxy.data.local.entity.LocationBasedAutoRun>>()  // 🆕
                
                groups.forEach { group ->
                    // 시간대 목록 로드
                    val linkedAutoRuns = repository.getLinkedTimeBasedAutoRuns(group.id)
                    timeBasedAutoRuns[group.id] = linkedAutoRuns
                    timeCounts[group.id] = linkedAutoRuns.size
                    
                    // 🆕 위치 목록 로드
                    val linkedLocations = repository.getLinkedLocations(group.id)
                    locations[group.id] = linkedLocations
                    locationCounts[group.id] = linkedLocations.size
                }
                
                _linkedTimeBasedAutoRuns.value = timeBasedAutoRuns
                _linkedTimeBasedAutoRunCounts.value = timeCounts
                _linkedLocations.value = locations  // 🆕
                _linkedLocationCounts.value = locationCounts
            } catch (e: Exception) {
                _errorState.value = "연결된 설정 조회 실패: ${e.message}"
            }
        }
    }
    
    /**
     * 🆕 특정 스케줄 그룹의 위치 정보 갱신
     * 
     * LocationEditDialog에서 위치 정보 수정 후 호출하여
     * 해당 그룹의 위치 정보를 다시 로드합니다.
     * 
     * @param groupId 갱신할 스케줄 그룹 ID
     */
    fun loadLinkedLocations(groupId: String) {
        viewModelScope.launch {
            try {
                // 해당 그룹의 위치 정보 갱신
                val linkedLocations = repository.getLinkedLocations(groupId)
                
                // 현재 Map을 복사하여 해당 그룹만 업데이트
                val updatedLocations = _linkedLocations.value.toMutableMap()
                updatedLocations[groupId] = linkedLocations
                _linkedLocations.value = updatedLocations
                
                // 위치 개수도 갱신
                val updatedCounts = _linkedLocationCounts.value.toMutableMap()
                updatedCounts[groupId] = linkedLocations.size
                _linkedLocationCounts.value = updatedCounts
            } catch (e: Exception) {
                _errorState.value = "위치 정보 갱신 실패: ${e.message}"
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
     * @param isLocationBased 위치기반 스케쥴 여부 (true: 위치 진입 시 활성화, false: 즉시 활성화)
     * @return 생성된 ScheduleGroup의 ID
     * @throws IllegalArgumentException 이름이 비어있거나 시간대가 없을 때
     */
    suspend fun createScheduleGroupWithTimeSlots(
        name: String,
        description: String?,
        timeSlots: List<TimeSlot>,
        isLocationBased: Boolean = false  // 🐛 버그 수정: 위치기반 여부에 따라 초기 활성화 상태 결정
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
            // 🐛 버그 수정: 위치기반 여부에 따라 초기 활성화 상태 결정
            // - 위치기반 스케줄: isActive = false (위치 진입 시 활성화)
            // - 일반 시간 스케줄: isActive = true (즉시 활성화)
            val scheduleGroup = ScheduleGroup(
                name = name.trim(),
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                isActive = !isLocationBased  // 🐛 버그 수정: 위치기반이면 false, 아니면 true
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
     * 1. 템플릿의 timeSlots를 복사하면서 각 TimeSlot의 enabledDays를 template.defaultDays로 덮어씀
     * 2. 템플릿의 description을 시간표 설명으로 사용
     * 3. 수정된 timeSlots로 createScheduleGroupWithTimeSlots 호출
     *
     * ## 중요: 요일 불일치 방지 (Phase 2.1 Review)
     * - ScheduleTemplate.defaultDays가 단일 소스 역할
     * - TimeSlot의 기본 enabledDays(매일)를 템플릿의 defaultDays로 덮어씀
     * - 예: 평일 템플릿 선택 시 모든 TimeSlot이 월~금만 활성화됨
     *
     * @param name 시간표 이름 (사용자 입력)
     * @param template 선택한 템플릿
     * @param isLocationBased 위치기반 스케쥴 여부 (true: 위치 진입 시 활성화, false: 즉시 활성화)
     * @return 생성된 ScheduleGroup의 ID
     * @throws IllegalArgumentException 이름이 비어있거나 템플릿에 시간대가 없을 때
     */
    suspend fun createFromTemplate(
        name: String,
        template: ScheduleTemplate,
        isLocationBased: Boolean = false  // 🐛 버그 수정: 위치기반 여부에 따라 초기 활성화 상태 결정
    ): String {
        // 템플릿의 defaultDays를 각 TimeSlot의 enabledDays로 적용
        val timeSlotsWithDays = template.timeSlots.map { slot ->
            slot.copy(enabledDays = template.defaultDays)
        }
        
        return createScheduleGroupWithTimeSlots(
            name = name,
            description = template.description,
            timeSlots = timeSlotsWithDays,
            isLocationBased = isLocationBased  // 🐛 버그 수정: 위치기반 여부 전달
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
     * 오늘 예정된 다음 스케줄 조회 (v0.10 UI/UX 개선, v0.10.1 멀티 그룹 지원)
     *
     * 모든 활성화된 스케줄 그룹에서 현재 시각 이후의 가장 가까운 시간대를 반환합니다.
     * 스케줄 탭의 "다음 예약" 카드에 표시됩니다.
     *
     * ## 동작 방식
     * 1. 활성화된 모든 스케줄 그룹 조회
     * 2. 각 그룹의 모든 시간대 조회
     * 3. 현재 시각 이후의 시간대만 필터링
     * 4. 시작 시간 순으로 정렬
     * 5. 가장 가까운 시간대 반환
     *
     * @return 다음 예약 시간대 (없으면 null)
     */
    fun getNextScheduleToday(): Flow<TimeBasedAutoRun?> = flow {
        val now = LocalTime.now()
        val currentActiveGroups = activeGroups.value
        
        if (currentActiveGroups.isNotEmpty()) {
            // 모든 활성화된 그룹의 시간대를 수집
            val allTimeSlots = mutableListOf<TimeBasedAutoRun>()
            currentActiveGroups.forEach { group ->
                val timeSlots = repository.getLinkedTimeBasedAutoRuns(group.id)
                allTimeSlots.addAll(timeSlots)
            }
            
            // 현재 시각 이후의 가장 가까운 시간대 찾기
            val nextSlot = allTimeSlots
                .filter { slot ->
                    val slotTime = parseTime(slot.hour, slot.minute)
                    slotTime > now
                }
                .sortedBy { slot -> slot.hour * 60 + slot.minute }
                .firstOrNull()
            
            emit(nextSlot)
        } else {
            emit(null)
        }
    }
    
    /**
     * 시간표의 시간대 개수 조회 (v0.10 UI/UX 개선)
     *
     * 스케줄 탭의 ScheduleSummaryCard에서 사용됩니다.
     * 실제로는 linkedTimeBasedAutoRuns StateFlow를 사용하는 것이 더 효율적입니다.
     *
     * @param groupId 스케줄 그룹 ID
     * @return 시간대 개수
     */
    fun getTimeSlotCount(groupId: String): Flow<Int> = 
        flow {
            val timeSlots = linkedTimeBasedAutoRuns.value[groupId] ?: emptyList()
            emit(timeSlots.size)
        }
    
    /**
     * 연결된 위치 이름 조회 (v0.10 UI/UX 개선)
     *
     * 스케줄 탭의 ScheduleSummaryCard에서 사용됩니다.
     * 실제로는 linkedLocations StateFlow를 사용하는 것이 더 효율적입니다.
     *
     * @param groupId 스케줄 그룹 ID
     * @return 첫 번째 연결된 위치 이름 (없으면 null)
     */
    fun getLinkedLocationName(groupId: String): Flow<String?> = flow {
        val locations = linkedLocations.value[groupId] ?: emptyList()
        emit(locations.firstOrNull()?.label)
    }
    
    /**
     * 시간 파싱 헬퍼 함수
     *
     * @param hour 시 (0-23)
     * @param minute 분 (0-59)
     * @return LocalTime 객체
     */
    private fun parseTime(hour: Int, minute: Int): LocalTime {
        return LocalTime.of(hour, minute)
    }

    /**
     * 에러 상태 초기화
     */
    fun clearError() {
        _errorState.value = null
    }
}

