package com.allday.detoxy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.core.utils.AnalyticsHelper
import com.allday.detoxy.data.repository.TimeBasedAutoRunRepository
import com.allday.detoxy.data.repository.UserSettingsRepository
import com.allday.detoxy.domain.manager.NextAutoRunCalculator
import com.allday.detoxy.domain.manager.NextAutoRunInfo
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
 * 자동 실행 대시보드 ViewModel
 *
 * 2.5차 고도화 Week 1, Day 4-6: AutoRunDashboardViewModel 확장
 *
 * ## 책임
 * - 다음 예정 자동 실행 정보 제공 (nextAutoRunInfo)
 * - 자동 실행 건너뛰기 (skipNextAutoRun)
 * - 수동 새로고침 (refreshNextAutoRun)
 * - 자동 실행 마스터 스위치 상태 제공
 * - 일시중지 상태 제공
 *
 * @param nextAutoRunCalculator 다음 자동 실행 계산기
 * @param timeBasedAutoRunRepository 시간 기반 자동 실행 저장소
 * @param autoRunSettingsRepository 자동 실행 설정 저장소
 * @param userSettingsRepository 사용자 설정 저장소
 * @param analyticsHelper Analytics 헬퍼
 *
 * @see NextAutoRunCalculator
 * @see NextAutoRunInfo
 * @see docs/02.5_autosetting_todolist.md §1.2.2
 * @see docs/02_advanced_wireframe_spec.md §5.1
 */
@HiltViewModel
class AutoRunDashboardViewModel @Inject constructor(
    private val nextAutoRunCalculator: NextAutoRunCalculator,
    private val timeBasedAutoRunRepository: TimeBasedAutoRunRepository,
    private val autoRunSettingsRepository: AutoRunSettingsRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val analyticsHelper: AnalyticsHelper
) : ViewModel() {

    // ========== 다음 예정 자동 실행 ==========

    /**
     * 다음 예정 자동 실행 정보
     */
    private val _nextAutoRunInfo = MutableStateFlow<NextAutoRunInfo?>(null)
    val nextAutoRunInfo: StateFlow<NextAutoRunInfo?> = _nextAutoRunInfo.asStateFlow()

    /**
     * 로딩 상태
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 에러 상태
     */
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    // ========== 자동 실행 제어 상태 ==========

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

    // ========== 글로벌 옵션 ==========

    /**
     * 주말 제외 설정
     */
    val excludeWeekends: StateFlow<Boolean> = autoRunSettingsRepository.excludeWeekendsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadAutoRunControlState()
        refreshNextAutoRun()
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
     * 다음 자동 실행 정보 새로고침
     *
     * 활성화된 시간 기반 자동 실행 목록을 조회하여
     * NextAutoRunCalculator로 다음 예정 자동 실행을 계산합니다.
     */
    fun refreshNextAutoRun() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorState.value = null

                // 활성화된 시간 기반 자동 실행 목록 조회
                timeBasedAutoRunRepository.getEnabled().collect { enabledAutoRuns ->
                    // 다음 예정 자동 실행 계산
                    val nextInfo = nextAutoRunCalculator.calculateNextAutoRun(enabledAutoRuns)
                    _nextAutoRunInfo.value = nextInfo

                    // Analytics 이벤트는 필요시 추가
                    // 현재는 nextInfo를 단순히 계산만 하고 로깅하지 않음
                }
            } catch (e: Exception) {
                _errorState.value = "다음 자동 실행 정보를 불러오지 못했습니다: ${e.message}"
                // 에러 로깅은 별도 구현 필요시 추가
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 이 회차 건너뛰기
     *
     * 다음 예정 자동 실행을 일시적으로 비활성화합니다.
     * 해당 자동 실행 설정의 다음 트리거 시각까지 일시중지합니다.
     */
    fun skipNextAutoRun() {
        viewModelScope.launch {
            try {
                val nextInfo = _nextAutoRunInfo.value
                if (nextInfo == null) {
                    _errorState.value = "건너뛸 자동 실행이 없습니다"
                    return@launch
                }

                // 다음 트리거 시각까지 일시중지
                val pauseUntil = nextInfo.triggerTime + (60 * 1000) // 트리거 시각 + 1분
                userSettingsRepository.setAutoRunPauseUntil(pauseUntil)
                _pauseUntil.value = pauseUntil

                // Analytics 이벤트
                analyticsHelper.logAutoRunSkipped(
                    triggerType = nextInfo.triggerType,
                    reason = "manual_dashboard_skip"
                )

                // UI 피드백을 위한 성공 메시지
                _errorState.value = null

                // 다음 자동 실행 정보 새로고침
                refreshNextAutoRun()

            } catch (e: Exception) {
                _errorState.value = "건너뛰기에 실패했습니다: ${e.message}"
                // 에러 로깅은 별도 구현 필요시 추가
            }
        }
    }

    /**
     * 마스터 스위치 토글
     *
     * @param enabled true: 모든 자동 실행 활성화, false: 비활성화
     */
    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userSettingsRepository.setAutoRunMasterEnabled(enabled)
                _masterEnabled.value = enabled

                // Analytics 이벤트는 필요시 추가
                // 현재는 토글만 수행

                // 상태 변경 후 다음 자동 실행 정보 새로고침
                refreshNextAutoRun()

            } catch (e: Exception) {
                _errorState.value = "마스터 스위치 변경에 실패했습니다: ${e.message}"
            }
        }
    }

    /**
     * 일시중지 설정
     *
     * @param pauseUntil 일시중지 해제 시각 (epoch millis), null이면 즉시 해제
     */
    fun setPauseUntil(pauseUntil: Long?) {
        viewModelScope.launch {
            try {
                userSettingsRepository.setAutoRunPauseUntil(pauseUntil)
                _pauseUntil.value = pauseUntil

                // Analytics 이벤트는 필요시 추가
                // 현재는 일시중지 설정만 수행

                // 상태 변경 후 다음 자동 실행 정보 새로고침
                refreshNextAutoRun()

            } catch (e: Exception) {
                _errorState.value = "일시중지 설정에 실패했습니다: ${e.message}"
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

