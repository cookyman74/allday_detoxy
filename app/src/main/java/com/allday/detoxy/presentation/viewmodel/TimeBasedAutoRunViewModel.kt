package com.allday.detoxy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.core.manager.AutoRunAlarmManager
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.data.repository.TimeBasedAutoRunRepository
import com.allday.detoxy.data.repository.UserSettingsRepository
import com.allday.detoxy.domain.repository.AutoRunSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
 */
@HiltViewModel
class TimeBasedAutoRunViewModel @Inject constructor(
    private val repository: TimeBasedAutoRunRepository,
    private val alarmManager: AutoRunAlarmManager,
    private val settingsRepository: AutoRunSettingsRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    // 시간 기반 자동 실행 목록
    private val _autoRuns = MutableStateFlow<List<TimeBasedAutoRun>>(emptyList())
    val autoRuns: StateFlow<List<TimeBasedAutoRun>> = _autoRuns.asStateFlow()

    // 정확 알람 권한 상태 (AlarmManager에서 가져옴)
    val canScheduleExactAlarms: StateFlow<Boolean> = alarmManager.canScheduleExactAlarms

    // 에러 상태
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

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
                repository.insert(autoRun)
                if (autoRun.isEnabled) {
                    alarmManager.scheduleTimeBasedAutoRun(autoRun)
                }
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
                repository.update(autoRun)
                // 알람 재등록
                alarmManager.cancelTimeBasedAutoRun(autoRun.id)
                if (autoRun.isEnabled) {
                    alarmManager.scheduleTimeBasedAutoRun(autoRun)
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
                alarmManager.cancelTimeBasedAutoRun(autoRunId)
                repository.delete(autoRunId)
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
                // 🔧 Critical Fix: stale data 문제 해결
                // DB 업데이트 전에 현재 엔티티를 복사하여 isEnabled 업데이트
                val currentAutoRun = _autoRuns.value.find { it.id == autoRunId }
                if (currentAutoRun == null) {
                    _errorState.value = "자동 실행을 찾을 수 없습니다"
                    return@launch
                }
                
                repository.setEnabled(autoRunId, isEnabled)
                
                // 활성화 시 알람 등록, 비활성화 시 알람 취소
                if (isEnabled) {
                    // 복사한 엔티티의 isEnabled를 업데이트하여 전달
                    alarmManager.scheduleTimeBasedAutoRun(currentAutoRun.copy(isEnabled = true))
                } else {
                    alarmManager.cancelTimeBasedAutoRun(autoRunId)
                }
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
}
