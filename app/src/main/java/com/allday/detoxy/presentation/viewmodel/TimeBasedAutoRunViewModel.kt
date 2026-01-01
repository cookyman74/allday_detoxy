package com.allday.detoxy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.core.manager.AutoRunAlarmManager
import com.allday.detoxy.core.utils.AnalyticsHelper
import com.allday.detoxy.data.local.entity.ScheduleGroup
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.data.repository.TimeBasedAutoRunRepository
import com.allday.detoxy.data.repository.UserSettingsRepository
import com.allday.detoxy.domain.repository.AutoRunSettingsRepository
import com.allday.detoxy.domain.repository.ScheduleGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 시간 기반 자동 실행 화면 ViewModel
 *
 * 시간 기반 자동 실행 설정, AlarmManager 연동, 권한 상태 관리, 글로벌 옵션 관리를 담당합니다.
 *
 * @param repository TimeBasedAutoRunRepository 인스턴스
 * @param alarmManager AutoRunAlarmManager 인스턴스
 * @param settingsRepository AutoRunSettingsRepository 인스턴스 (글로벌 옵션)
 * @param userSettingsRepository UserSettingsRepository 인스턴스 (마스터 토글, 일시중지)
 * @param scheduleGroupRepository ScheduleGroupRepository 인스턴스 (3차 고도화: 시간표 연동)
 */
@HiltViewModel
class TimeBasedAutoRunViewModel @Inject constructor(
    private val repository: TimeBasedAutoRunRepository,
    private val alarmManager: AutoRunAlarmManager,
    private val settingsRepository: AutoRunSettingsRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val scheduleGroupRepository: ScheduleGroupRepository  // 🆕 3차 고도화
) : ViewModel() {

    // 시간 기반 자동 실행 목록
    private val _autoRuns = MutableStateFlow<List<TimeBasedAutoRun>>(emptyList())
    val autoRuns: StateFlow<List<TimeBasedAutoRun>> = _autoRuns.asStateFlow()

    // 🆕 3차 고도화: 스케줄 그룹 맵 (ID -> ScheduleGroup)
    private val _scheduleGroupMap = MutableStateFlow<Map<String, ScheduleGroup>>(emptyMap())
    val scheduleGroupMap: StateFlow<Map<String, ScheduleGroup>> = _scheduleGroupMap.asStateFlow()

    // 정확 알람 권한 상태 (AlarmManager에서 가져옴)
    val canScheduleExactAlarms: StateFlow<Boolean> = alarmManager.canScheduleExactAlarms

    // 에러 상태
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    // ==================== v1.1: 히트맵 갱신 이벤트 ====================

    /**
     * 시간대 변경 시 발행되는 이벤트
     *
     * 시간대 추가/수정/삭제/토글 후 scheduleGroupId를 emit합니다.
     * ScheduleGroupViewModel.refreshHeatmapData()를 호출하여 히트맵을 갱신할 수 있습니다.
     */
    private val _timeSlotUpdated = MutableSharedFlow<String>()
    val timeSlotUpdated: SharedFlow<String> = _timeSlotUpdated.asSharedFlow()

    // ==================== 글로벌 옵션 ====================

    /**
     * 주말 제외 설정
     */
    val excludeWeekends: StateFlow<Boolean> = settingsRepository.excludeWeekendsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * 자동 시작 딜레이 (분)
     */
    val autoStartDelayMinutes: StateFlow<Int> = settingsRepository.autoStartDelayMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * 사전 알림 시간 (분)
     */
    val preNotificationMinutes: StateFlow<Int> = settingsRepository.preNotificationMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    // ==================== 자동 실행 제어 (v4) ====================

    /**
     * 자동 실행 마스터 스위치 상태
     */
    private val _masterEnabled = MutableStateFlow(true)
    val masterEnabled: StateFlow<Boolean> = _masterEnabled.asStateFlow()

    /**
     * 자동 실행 일시중지 해제 시각
     */
    private val _pauseUntil = MutableStateFlow<Long?>(null)
    val pauseUntil: StateFlow<Long?> = _pauseUntil.asStateFlow()

    init {
        loadAutoRuns()
        loadAutoRunControlState()
        loadScheduleGroups()  // 🆕 3차 고도화: 시간표 그룹 로드
    }

    /**
     * 자동 실행 제어 상태 로드
     */
    private fun loadAutoRunControlState() {
        viewModelScope.launch {
            _masterEnabled.value = userSettingsRepository.getAutoRunMasterEnabled()
            _pauseUntil.value = userSettingsRepository.getAutoRunPauseUntil()
        }
    }

    /**
     * 마스터 스위치 설정
     */
    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.setAutoRunMasterEnabled(enabled)
            _masterEnabled.value = enabled
            
            // 마스터 스위치를 켜면 모든 활성화된 자동 실행 재등록
            if (enabled) {
                _autoRuns.value.filter { it.isEnabled }.forEach { autoRun ->
                    alarmManager.scheduleTimeBasedAutoRun(autoRun)
                }
            }
        }
    }

    /**
     * N시간 동안 일시중지
     */
    fun pauseForHours(hours: Int) {
        viewModelScope.launch {
            userSettingsRepository.pauseForHours(hours)
            _pauseUntil.value = userSettingsRepository.getAutoRunPauseUntil()
        }
    }

    /**
     * 오늘 하루 중지 (자정까지)
     */
    fun pauseUntilMidnight() {
        viewModelScope.launch {
            userSettingsRepository.pauseUntilMidnight()
            _pauseUntil.value = userSettingsRepository.getAutoRunPauseUntil()
        }
    }

    /**
     * 일시중지 해제
     */
    fun resumeAutoRun() {
        viewModelScope.launch {
            userSettingsRepository.resumeAutoRun()
            _pauseUntil.value = null
        }
    }

    /**
     * 모든 시간 기반 자동 실행 목록 로드
     */
    private fun loadAutoRuns() {
        viewModelScope.launch {
            repository.getAll().collect { autoRuns ->
                _autoRuns.value = autoRuns.sortedWith(
                    compareBy<TimeBasedAutoRun> { it.hour }
                        .thenBy { it.minute }
                )
            }
        }
    }

    /**
     * 새로운 시간 기반 자동 실행 추가
     *
     * @param autoRun 추가할 TimeBasedAutoRun
     */
    fun addAutoRun(autoRun: TimeBasedAutoRun) {
        viewModelScope.launch {
            try {
                // 디버깅 로그
                android.util.Log.d("TimeBasedAutoRunViewModel", "=== addAutoRun ===")
                android.util.Log.d("TimeBasedAutoRunViewModel", "autoRun.id: ${autoRun.id}")
                android.util.Log.d("TimeBasedAutoRunViewModel", "autoRun.scheduleGroupId: ${autoRun.scheduleGroupId}")
                android.util.Log.d("TimeBasedAutoRunViewModel", "autoRun.isIndependent: ${autoRun.isIndependent}")
                android.util.Log.d("TimeBasedAutoRunViewModel", "autoRun.isEnabled: ${autoRun.isEnabled}")
                
                repository.insert(autoRun)
                if (autoRun.isEnabled) {
                    val scheduled = alarmManager.scheduleTimeBasedAutoRun(autoRun)
                    android.util.Log.d("TimeBasedAutoRunViewModel", "알람 등록 결과: $scheduled")
                }
                
                // v1.1: 히트맵 갱신 이벤트 발행
                autoRun.scheduleGroupId?.let { groupId ->
                    _timeSlotUpdated.emit(groupId)
                }
                
                // Analytics 로깅
                val enabledDaysCount = try {
                    // enabledDays는 JSON 배열 문자열 (예: "[\"MON\",\"TUE\"]")
                    autoRun.enabledDays.count { it == ',' } + 1
                } catch (e: Exception) {
                    1
                }
                AnalyticsHelper.logTimeBasedAutoRunCreated(
                    hour = autoRun.hour,
                    minute = autoRun.minute,
                    durationMinutes = autoRun.durationMinutes,
                    presetType = autoRun.presetType,
                    enabledDaysCount = enabledDaysCount,
                    hasLabel = !autoRun.label.isNullOrEmpty(),
                    isFromTemplate = false,
                    templateType = null
                )
            } catch (e: Exception) {
                _errorState.value = "자동 실행 추가 실패: ${e.message}"
            }
        }
    }

    /**
     * 시간 기반 자동 실행 업데이트
     *
     * @param autoRun 업데이트할 TimeBasedAutoRun
     */
    fun updateAutoRun(autoRun: TimeBasedAutoRun) {
        viewModelScope.launch {
            try {
                // 디버깅 로그
                android.util.Log.d("TimeBasedAutoRunViewModel", "=== updateAutoRun ===")
                android.util.Log.d("TimeBasedAutoRunViewModel", "autoRun.id: ${autoRun.id}")
                android.util.Log.d("TimeBasedAutoRunViewModel", "autoRun.scheduleGroupId: ${autoRun.scheduleGroupId}")
                android.util.Log.d("TimeBasedAutoRunViewModel", "autoRun.isIndependent: ${autoRun.isIndependent}")
                android.util.Log.d("TimeBasedAutoRunViewModel", "autoRun.isEnabled: ${autoRun.isEnabled}")
                
                repository.update(autoRun)
                // 알람 재등록
                alarmManager.cancelTimeBasedAutoRun(autoRun.id)
                if (autoRun.isEnabled) {
                    val scheduled = alarmManager.scheduleTimeBasedAutoRun(autoRun)
                    android.util.Log.d("TimeBasedAutoRunViewModel", "알람 등록 결과: $scheduled")
                }
                
                // v1.1: 히트맵 갱신 이벤트 발행
                autoRun.scheduleGroupId?.let { groupId ->
                    _timeSlotUpdated.emit(groupId)
                }
            } catch (e: Exception) {
                _errorState.value = "자동 실행 수정 실패: ${e.message}"
            }
        }
    }

    /**
     * 시간 기반 자동 실행 삭제
     *
     * @param autoRunId 삭제할 자동 실행 ID
     */
    fun deleteAutoRun(autoRunId: String) {
        viewModelScope.launch {
            try {
                // Analytics 로깅용 데이터 수집 (삭제 전)
                val autoRun = _autoRuns.value.find { it.id == autoRunId }
                val groupId = autoRun?.scheduleGroupId  // v1.1: 히트맵 갱신용
                
                alarmManager.cancelTimeBasedAutoRun(autoRunId)
                repository.delete(autoRunId)
                
                // v1.1: 히트맵 갱신 이벤트 발행
                groupId?.let { _timeSlotUpdated.emit(it) }
                
                // Analytics 로깅
                if (autoRun != null) {
                    val daysActive = ((System.currentTimeMillis() - autoRun.createdAt) / (1000 * 60 * 60 * 24)).toInt()
                    AnalyticsHelper.logTimeBasedAutoRunDeleted(
                        usageCount = 0,
                        daysActive = daysActive
                    )
                }
            } catch (e: Exception) {
                _errorState.value = "자동 실행 삭제 실패: ${e.message}"
            }
        }
    }

    /**
     * 시간 기반 자동 실행 활성화/비활성화 토글
     *
     * @param autoRunId 토글할 자동 실행 ID
     * @param isEnabled 활성화 여부
     */
    fun toggleAutoRun(autoRunId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                // stale data 문제 해결: DB 업데이트 전에 현재 엔티티를 복사
                val currentAutoRun = _autoRuns.value.find { it.id == autoRunId }
                if (currentAutoRun == null) {
                    _errorState.value = "자동 실행을 찾을 수 없습니다"
                    return@launch
                }
                
                repository.setEnabled(autoRunId, isEnabled)
                
                // 활성화 시 알람 등록, 비활성화 시 알람 취소
                if (isEnabled) {
                    alarmManager.scheduleTimeBasedAutoRun(currentAutoRun.copy(isEnabled = true))
                } else {
                    alarmManager.cancelTimeBasedAutoRun(autoRunId)
                }
                
                // v1.1: 히트맵 갱신 이벤트 발행
                currentAutoRun.scheduleGroupId?.let { groupId ->
                    _timeSlotUpdated.emit(groupId)
                }
                
                // Analytics 로깅
                val totalEnabledCount = _autoRuns.value.count { it.isEnabled || (it.id == autoRunId && isEnabled) }
                AnalyticsHelper.logTimeBasedAutoRunToggled(
                    isEnabled = isEnabled,
                    totalEnabledCount = totalEnabledCount
                )
            } catch (e: Exception) {
                _errorState.value = "활성화 변경 실패: ${e.message}"
            }
        }
    }

    /**
     * 에러 상태 초기화
     */
    fun clearError() {
        _errorState.value = null
    }

    /**
     * 정확 알람 권한 상태 갱신
     * 
     * 화면이 포그라운드로 돌아올 때 권한 상태를 다시 확인합니다.
     * 권한 설정 화면에서 돌아온 경우 권한 상태가 변경되었을 수 있으므로
     * 이 메서드를 호출하여 StateFlow를 업데이트합니다.
     */
    fun refreshExactAlarmPermission() {
        alarmManager.canScheduleExactAlarms()
    }

    // ==================== 글로벌 옵션 저장 ====================

    /**
     * 주말 제외 설정 저장
     *
     * @param exclude true: 주말에 자동 실행 안 함, false: 주말에도 자동 실행
     */
    fun setExcludeWeekends(exclude: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.saveExcludeWeekends(exclude)
            } catch (e: Exception) {
                _errorState.value = "설정 저장 실패: ${e.message}"
            }
        }
    }

    /**
     * 자동 시작 딜레이 저장
     *
     * @param minutes 0, 5, 10 중 하나
     */
    fun setAutoStartDelayMinutes(minutes: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.saveAutoStartDelayMinutes(minutes)
            } catch (e: Exception) {
                _errorState.value = "설정 저장 실패: ${e.message}"
            }
        }
    }

    /**
     * 사전 알림 시간 저장
     *
     * @param minutes 0, 5, 10, 15 중 하나
     */
    fun setPreNotificationMinutes(minutes: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.savePreNotificationMinutes(minutes)
            } catch (e: Exception) {
                _errorState.value = "설정 저장 실패: ${e.message}"
            }
        }
    }

    // ==================== 3차 고도화: 시간표 그룹 ====================

    /**
     * 시간표 그룹 로드
     *
     * 3차 고도화: TimeBasedAutoRun에 연결된 ScheduleGroup 정보를 가져옵니다.
     */
    private fun loadScheduleGroups() {
        viewModelScope.launch {
            scheduleGroupRepository.getAll().collect { groups ->
                _scheduleGroupMap.value = groups.associateBy { it.id }
            }
        }
    }
}
