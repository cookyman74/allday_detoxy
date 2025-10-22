package com.allday.detoxy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.core.manager.AutoRunAlarmManager
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.data.repository.TimeBasedAutoRunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 시간 기반 자동 실행 화면 ViewModel
 *
 * 시간 기반 자동 실행 설정, AlarmManager 연동, 권한 상태 관리를 담당합니다.
 *
 * @param repository TimeBasedAutoRunRepository 인스턴스
 * @param alarmManager AutoRunAlarmManager 인스턴스
 */
@HiltViewModel
class TimeBasedAutoRunViewModel @Inject constructor(
    private val repository: TimeBasedAutoRunRepository,
    private val alarmManager: AutoRunAlarmManager
) : ViewModel() {

    // 시간 기반 자동 실행 목록
    private val _autoRuns = MutableStateFlow<List<TimeBasedAutoRun>>(emptyList())
    val autoRuns: StateFlow<List<TimeBasedAutoRun>> = _autoRuns.asStateFlow()

    // 정확 알람 권한 상태 (AlarmManager에서 가져옴)
    val canScheduleExactAlarms: StateFlow<Boolean> = alarmManager.canScheduleExactAlarms

    init {
        loadAutoRuns()
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
            repository.insert(autoRun)
            if (autoRun.isEnabled) {
                alarmManager.scheduleTimeBasedAutoRun(autoRun)
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
            repository.update(autoRun)
            // 알람 재등록
            alarmManager.cancelTimeBasedAutoRun(autoRun.id)
            if (autoRun.isEnabled) {
                alarmManager.scheduleTimeBasedAutoRun(autoRun)
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
            repository.delete(autoRunId)
            alarmManager.cancelTimeBasedAutoRun(autoRunId)
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
            repository.toggleEnabled(autoRunId, isEnabled)
            
            // 활성화 시 알람 등록, 비활성화 시 알람 취소
            if (isEnabled) {
                val autoRun = _autoRuns.value.find { it.id == autoRunId }
                autoRun?.let { alarmManager.scheduleTimeBasedAutoRun(it) }
            } else {
                alarmManager.cancelTimeBasedAutoRun(autoRunId)
            }
        }
    }
}

