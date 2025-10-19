package com.allday.detoxy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.domain.manager.DetoxyAdvancedStatistics
import com.allday.detoxy.domain.repository.FocusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 리포트 화면 ViewModel
 *
 * Week 2B: Task 2B.3.1 - 고급 통계 통합
 * Week 2B: Task 2B.3.1 Review Fix - Clean Architecture 준수 및 Flow 수집 최적화
 */
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: FocusRepository,
    private val advancedStatistics: DetoxyAdvancedStatistics
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    // 하위 호환성을 위한 기존 API (Task 2B.3.2에서 제거 예정)
    val todaySessions: StateFlow<List<com.allday.detoxy.data.local.entity.FocusSession>>
        get() = uiState.map { it.todaySessions }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    val settings: StateFlow<com.allday.detoxy.data.local.entity.UserSettings>
        get() = uiState.map { it.settings }.stateIn(viewModelScope, SharingStarted.Eagerly, com.allday.detoxy.data.local.entity.UserSettings(1, 0, 0, null))
    
    val isLoading: StateFlow<Boolean>
        get() = uiState.map { it.isLoading }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        loadReportData()
    }

    /**
     * 리포트 데이터 로드 (기본 + 고급 통계)
     *
     * Review Fix: Flow 수집은 init에서 한 번만 실행되도록 개선
     */
    private fun loadReportData() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        // 1. 오늘 세션 로드 (Flow 관찰) - init에서 한 번만 수집
        viewModelScope.launch {
            try {
                repository.getTodaySessions().collect { todaySessions ->
                    _uiState.update { it.copy(todaySessions = todaySessions) }
                    
                    // 첫 데이터 로드 완료
                    if (_uiState.value.isLoading) {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "세션 데이터 로드 실패: ${e.message}"
                    )
                }
            }
        }

        // 2. 사용자 설정 로드 (Flow 관찰) - init에서 한 번만 수집
        viewModelScope.launch {
            try {
                repository.getSettings().collect { userSettings ->
                    userSettings?.let { settings ->
                        _uiState.update { it.copy(settings = settings) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "설정 데이터 로드 실패: ${e.message}") 
                }
            }
        }

        // 3. 고급 통계 로드 (최근 7일)
        loadAdvancedStatistics()
    }

    /**
     * 고급 통계 계산 및 로드 (최근 7일 기준)
     *
     * Week 2B: Task 2B.3.1
     * Review Fix: Repository를 통해 차단 이벤트 조회 (Clean Architecture 준수)
     */
    private fun loadAdvancedStatistics() {
        viewModelScope.launch {
            try {
                // 최근 7일 세션 데이터 로드
                val now = System.currentTimeMillis()
                val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)
                val recentSessions = repository.getSessionsInRange(sevenDaysAgo, now)
                
                // 최근 7일 차단 이벤트 로드 (Review Fix: Repository를 통해 조회)
                val recentInterruptions = repository.getInterruptionsInLastDays(7)
                
                // 데이터가 충분한지 확인
                val hasData = recentSessions.isNotEmpty()
                
                if (hasData) {
                    // 종합 인사이트 계산
                    val insights = advancedStatistics.calculateComprehensiveInsights(
                        sessions = recentSessions,
                        interruptions = recentInterruptions
                    )
                    
                    // UI 상태 업데이트
                    _uiState.update {
                        it.copy(
                            hasData = true,
                            riskIndex = insights.riskIndex,
                            recoveryTrend = insights.recoveryTrend,
                            topDistractions = insights.topDistractions,
                            resistanceAnalysis = insights.resistanceAnalysis,
                            giveUpAnalysis = insights.giveUpAnalysis,
                            coachRecommendation = insights.coachRecommendation
                        )
                    }
                } else {
                    // 데이터 없음 (빈 상태)
                    _uiState.update { 
                        it.copy(
                            hasData = false,
                            riskIndex = null,
                            recoveryTrend = null,
                            topDistractions = emptyList(),
                            resistanceAnalysis = null,
                            giveUpAnalysis = null,
                            coachRecommendation = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        hasData = false,
                        error = "고급 통계 로드 실패: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 리포트 새로고침
     *
     * Review Fix: 고급 통계만 재계산 (Flow 수집은 이미 진행 중)
     */
    fun refresh() {
        loadAdvancedStatistics()
    }

    // 하위 호환성을 위한 기존 메서드들 (Task 2B.3.2에서 제거 예정)
    fun getSuccessSessionCount(): Int = _uiState.value.getSuccessSessionCount()
    fun getTotalFocusMinutes(): Int = _uiState.value.getTotalFocusMinutes()
    fun getFailedSessionCount(): Int = _uiState.value.getFailedSessionCount()
    fun getSuccessRate(): Float = _uiState.value.getSuccessRate()
}
