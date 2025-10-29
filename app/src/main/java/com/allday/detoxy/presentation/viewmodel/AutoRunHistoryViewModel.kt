package com.allday.detoxy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.data.local.entity.AutoRunLog
import com.allday.detoxy.domain.manager.AutoRunStatistics
import com.allday.detoxy.domain.manager.AutoRunStatisticsCalculator
import com.allday.detoxy.domain.repository.IAutoRunLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * 자동 실행 이력 ViewModel
 *
 * 2.5차 고도화 Week 2, Day 11-13: AutoRunHistoryScreen 구현
 *
 * ## Clean Architecture 준수
 * - domain 레이어의 인터페이스(IAutoRunLogRepository)에 의존
 * - data 레이어의 구현체(DAO)에 직접 의존하지 않음
 *
 * ## 책임
 * - 자동 실행 이력 로드
 * - 필터링 (트리거 타입, 결과)
 * - 날짜 범위 선택
 * - 기본 통계 제공
 *
 * @param autoRunLogRepository 자동 실행 로그 Repository (인터페이스)
 * @param autoRunStatisticsCalculator 통계 계산기
 *
 * @see AutoRunLog
 * @see AutoRunStatistics
 * @see IAutoRunLogRepository
 */
@HiltViewModel
class AutoRunHistoryViewModel @Inject constructor(
    private val autoRunLogRepository: IAutoRunLogRepository,
    private val autoRunStatisticsCalculator: AutoRunStatisticsCalculator
) : ViewModel() {

    // ========== 필터 상태 ==========

    /**
     * 트리거 타입 필터
     * - ALL: 전체
     * - TIME: 시간 기반
     * - LOCATION: 위치 기반
     */
    private val _triggerTypeFilter = MutableStateFlow("ALL")
    val triggerTypeFilter: StateFlow<String> = _triggerTypeFilter.asStateFlow()

    /**
     * 결과 필터
     * - ALL: 전체
     * - STARTED: 시작됨
     * - SKIPPED: 건너뜀
     * - FAILED: 실패
     */
    private val _resultFilter = MutableStateFlow("ALL")
    val resultFilter: StateFlow<String> = _resultFilter.asStateFlow()

    /**
     * 날짜 범위 선택
     * - WEEK: 이번 주
     * - MONTH: 이번 달
     * - ALL: 전체
     */
    private val _dateRangeFilter = MutableStateFlow("WEEK")
    val dateRangeFilter: StateFlow<String> = _dateRangeFilter.asStateFlow()

    // ========== 데이터 로드 ==========

    /**
     * 모든 자동 실행 로그
     */
    private val allLogs: StateFlow<List<AutoRunLog>> = autoRunLogRepository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * 필터링된 로그
     */
    val filteredLogs: StateFlow<Map<String, List<AutoRunLog>>> = combine(
        allLogs,
        _triggerTypeFilter,
        _resultFilter,
        _dateRangeFilter
    ) { logs, triggerType, result, dateRange ->
        // 날짜 범위 필터링
        val dateFilteredLogs = filterByDateRange(logs, dateRange)

        // 트리거 타입 필터링
        val triggerFiltered = if (triggerType == "ALL") {
            dateFilteredLogs
        } else {
            dateFilteredLogs.filter { it.triggerType == triggerType }
        }

        // 결과 필터링
        val resultFiltered = if (result == "ALL") {
            triggerFiltered
        } else {
            triggerFiltered.filter { it.result == result }
        }

        // 날짜별 그룹핑
        groupByDate(resultFiltered)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    /**
     * 로딩 상태
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 통계 데이터
     */
    private val _statistics = MutableStateFlow<AutoRunStatistics?>(null)
    val statistics: StateFlow<AutoRunStatistics?> = _statistics.asStateFlow()

    init {
        loadStatistics()
    }

    // ========== 필터 메서드 ==========

    /**
     * 트리거 타입 필터 변경
     *
     * @param filterType 필터 타입 (ALL, TIME, LOCATION)
     */
    fun setTriggerTypeFilter(filterType: String) {
        _triggerTypeFilter.value = filterType
    }

    /**
     * 결과 필터 변경
     *
     * @param filterType 필터 타입 (ALL, STARTED, SKIPPED, FAILED)
     */
    fun setResultFilter(filterType: String) {
        _resultFilter.value = filterType
    }

    /**
     * 날짜 범위 필터 변경
     *
     * @param range 날짜 범위 (WEEK, MONTH, ALL)
     */
    fun setDateRangeFilter(range: String) {
        _dateRangeFilter.value = range
    }

    /**
     * 필터 초기화
     */
    fun resetFilters() {
        _triggerTypeFilter.value = "ALL"
        _resultFilter.value = "ALL"
        _dateRangeFilter.value = "WEEK"
    }

    // ========== 통계 로드 ==========

    /**
     * 통계 로드
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val stats = autoRunStatisticsCalculator.calculateStatistics()
                _statistics.value = stats
            } catch (e: Exception) {
                // 에러 발생 시 null 유지
                _statistics.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 통계 새로고침
     */
    fun refreshStatistics() {
        loadStatistics()
    }

    // ========== 헬퍼 메서드 ==========

    /**
     * 날짜 범위로 필터링
     *
     * @param logs 로그 리스트
     * @param range 날짜 범위 (WEEK, MONTH, ALL)
     * @return 필터링된 로그 리스트
     */
    private fun filterByDateRange(logs: List<AutoRunLog>, range: String): List<AutoRunLog> {
        val calendar = Calendar.getInstance()

        return when (range) {
            "WEEK" -> {
                // 이번 주 (일요일 00:00:00부터)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val weekStart = calendar.timeInMillis
                logs.filter { it.triggerTime >= weekStart }
            }
            "MONTH" -> {
                // 이번 달 (1일 00:00:00부터)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.timeInMillis
                logs.filter { it.triggerTime >= monthStart }
            }
            else -> logs // ALL
        }
    }

    /**
     * 날짜별 그룹핑
     *
     * @param logs 로그 리스트
     * @return 날짜별로 그룹핑된 맵 (키: 날짜, 값: 로그 리스트)
     */
    private fun groupByDate(logs: List<AutoRunLog>): Map<String, List<AutoRunLog>> {
        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 (E)", Locale.KOREAN)
        return logs
            .groupBy { log ->
                dateFormat.format(Date(log.triggerTime))
            }
            .toSortedMap(compareByDescending { it }) // 최신순 정렬
    }
}

