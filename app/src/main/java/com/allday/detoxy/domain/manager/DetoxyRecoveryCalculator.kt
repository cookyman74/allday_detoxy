package com.allday.detoxy.domain.manager

import com.allday.detoxy.data.local.entity.FocusSession
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.SortedMap
import java.util.TreeMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 디톡시 회복률(Recovery Rate) 추세 계산기
 *
 * 최근 7일/30일 기준 성공 세션 비율과 허용 앱 체류 감소율을 기반으로
 * 회복률 변화 그래프 및 주간 변화량을 계산합니다.
 *
 * ## 회복률 산식
 * - 기본: 성공 세션 비율 (%)
 * - 보정: 허용 앱 체류 시간 감소율 반영 (추후 FocusDistraction 연동)
 *
 * ## 추세 분석
 * - 일별 회복률 계산
 * - 주간 변화량 (현재 주 vs 이전 주)
 * - 성장/정체/하락 판단
 *
 * Week 2B: Task 2B.2
 */
@Singleton
class DetoxyRecoveryCalculator @Inject constructor() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(ZoneId.systemDefault())

    /**
     * 회복률 추세 계산 (7일 또는 30일)
     *
     * @param sessions 분석할 FocusSession 리스트 (전체 데이터)
     * @param periodDays 분석 기간 (7 또는 30)
     * @return DetoxyRecoveryTrend 객체
     */
    fun calculateRecoveryTrend(
        sessions: List<FocusSession>,
        periodDays: Int = 7
    ): DetoxyRecoveryTrend {
        if (sessions.isEmpty()) {
            return DetoxyRecoveryTrend(
                overallRate = 0f,
                dailyRates = sortedMapOf(),
                weeklyChange = 0f,
                trend = RecoveryTrendType.STABLE
            )
        }

        // periodDays 기간에 해당하는 세션만 필터링
        val now = System.currentTimeMillis()
        val periodStartTime = now - (periodDays * 24 * 60 * 60 * 1000L)
        val filteredSessions = sessions.filter { it.startTime >= periodStartTime }

        if (filteredSessions.isEmpty()) {
            return DetoxyRecoveryTrend(
                overallRate = 0f,
                dailyRates = sortedMapOf(),
                weeklyChange = 0f,
                trend = RecoveryTrendType.STABLE
            )
        }

        // 일별 회복률 계산
        val dailyRates = calculateDailyRecoveryRates(filteredSessions)

        // 전체 회복률
        val overallRate = calculateOverallRecoveryRate(filteredSessions)

        // 주간 변화량 (최근 7일 vs 이전 7일)
        // 최소 14일 이상의 데이터가 있을 때만 계산
        val weeklyChange = if (sessions.any { it.startTime >= now - (14 * 24 * 60 * 60 * 1000L) }) {
            calculateWeeklyChange(sessions)
        } else {
            0f
        }

        // 추세 판단
        val trend = determineTrend(weeklyChange)

        return DetoxyRecoveryTrend(
            overallRate = overallRate,
            dailyRates = dailyRates,
            weeklyChange = weeklyChange,
            trend = trend
        )
    }

    /**
     * 전체 회복률 계산 (%)
     */
    private fun calculateOverallRecoveryRate(sessions: List<FocusSession>): Float {
        if (sessions.isEmpty()) return 0f
        val successCount = sessions.count { it.success }
        return (successCount.toFloat() / sessions.size) * 100
    }

    /**
     * 일별 회복률 계산
     *
     * @return SortedMap<날짜(YYYY-MM-DD), 회복률(%)>
     */
    private fun calculateDailyRecoveryRates(sessions: List<FocusSession>): SortedMap<String, Float> {
        val dailyRates = TreeMap<String, Float>()

        // 날짜별 세션 그룹화
        val sessionsByDate = sessions.groupBy { session ->
            Instant.ofEpochMilli(session.startTime)
                .atZone(ZoneId.systemDefault())
                .format(dateFormatter)
        }

        // 각 날짜의 회복률 계산
        sessionsByDate.forEach { (date, daySessions) ->
            val successCount = daySessions.count { it.success }
            val rate = (successCount.toFloat() / daySessions.size) * 100
            dailyRates[date] = rate
        }

        return dailyRates
    }

    /**
     * 주간 변화량 계산 (%)
     *
     * 최근 7일 평균 회복률 - 이전 7일 평균 회복률
     */
    private fun calculateWeeklyChange(sessions: List<FocusSession>): Float {
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)
        val fourteenDaysAgo = now - (14 * 24 * 60 * 60 * 1000L)

        // 최근 7일 세션
        val recentSessions = sessions.filter { it.startTime >= sevenDaysAgo }
        val recentRate = calculateOverallRecoveryRate(recentSessions)

        // 이전 7일 세션
        val previousSessions = sessions.filter {
            it.startTime >= fourteenDaysAgo && it.startTime < sevenDaysAgo
        }
        val previousRate = calculateOverallRecoveryRate(previousSessions)

        return recentRate - previousRate
    }

    /**
     * 추세 판단
     *
     * @param weeklyChange 주간 변화량 (%)
     * @return RecoveryTrendType
     */
    private fun determineTrend(weeklyChange: Float): RecoveryTrendType {
        return when {
            weeklyChange > 10f -> RecoveryTrendType.IMPROVING  // 10% 이상 증가
            weeklyChange < -10f -> RecoveryTrendType.DECLINING // 10% 이상 감소
            else -> RecoveryTrendType.STABLE                   // -10% ~ 10%
        }
    }

    /**
     * 월별 회복률 비교 (30일 기준)
     *
     * @param sessions 분석할 FocusSession 리스트
     * @return DetoxyMonthlyComparison 객체
     */
    fun calculateMonthlyComparison(sessions: List<FocusSession>): DetoxyMonthlyComparison {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000L)
        val sixtyDaysAgo = now - (60 * 24 * 60 * 60 * 1000L)

        // 최근 30일 세션
        val recentSessions = sessions.filter { it.startTime >= thirtyDaysAgo }
        val recentRate = calculateOverallRecoveryRate(recentSessions)

        // 이전 30일 세션
        val previousSessions = sessions.filter {
            it.startTime >= sixtyDaysAgo && it.startTime < thirtyDaysAgo
        }
        val previousRate = calculateOverallRecoveryRate(previousSessions)

        val change = recentRate - previousRate

        return DetoxyMonthlyComparison(
            currentMonthRate = recentRate,
            previousMonthRate = previousRate,
            change = change,
            trend = determineTrend(change)
        )
    }
}

/**
 * 회복률 추세 데이터
 */
data class DetoxyRecoveryTrend(
    val overallRate: Float,                        // 전체 회복률 (%)
    val dailyRates: SortedMap<String, Float>,      // 일별 회복률 (날짜 → %)
    val weeklyChange: Float,                       // 주간 변화량 (%)
    val trend: RecoveryTrendType                   // 추세
)

/**
 * 월별 회복률 비교 데이터
 */
data class DetoxyMonthlyComparison(
    val currentMonthRate: Float,    // 이번 달 회복률 (%)
    val previousMonthRate: Float,   // 지난 달 회복률 (%)
    val change: Float,              // 변화량 (%)
    val trend: RecoveryTrendType    // 추세
)

/**
 * 회복률 추세 유형
 */
enum class RecoveryTrendType {
    IMPROVING,  // 개선 중 (10% 이상 증가)
    STABLE,     // 안정 (-10% ~ 10%)
    DECLINING;  // 하락 중 (10% 이상 감소)

    fun toDisplayString(): String = when (this) {
        IMPROVING -> "개선 중"
        STABLE -> "안정"
        DECLINING -> "하락 중"
    }

    fun getIcon(): String = when (this) {
        IMPROVING -> "↗️"
        STABLE -> "→"
        DECLINING -> "↘️"
    }
}

