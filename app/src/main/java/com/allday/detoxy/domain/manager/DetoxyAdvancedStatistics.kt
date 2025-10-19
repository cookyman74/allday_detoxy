package com.allday.detoxy.domain.manager

import com.allday.detoxy.data.local.entity.FocusInterruption
import com.allday.detoxy.data.local.entity.FocusSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 디톡시 고급 통계 통합 관리자
 *
 * 모든 고급 통계 계산기를 통합하여 종합적인 인사이트 제공:
 * - 위험 지수
 * - 회복률 추세
 * - 방해요인 분석
 * - 코치 추천
 *
 * Week 2B: Task 2B.2
 *
 * ## 사용 예시
 * ```kotlin
 * val statistics = detoxyAdvancedStatistics.calculateComprehensiveInsights(
 *     sessions = recentSessions,
 *     interruptions = recentInterruptions
 * )
 * ```
 */
@Singleton
class DetoxyAdvancedStatistics @Inject constructor(
    private val riskCalculator: DetoxyRiskCalculator,
    private val recoveryCalculator: DetoxyRecoveryCalculator,
    private val interruptionAnalyzer: FocusInterruptionAnalyzer,
    private val coachRecommender: DetoxyCoachRecommender,
    private val basicStatisticsCalculator: FocusStatisticsCalculator
) {

    /**
     * 종합 인사이트 계산 (최근 7일 기준)
     *
     * @param sessions 최근 세션 리스트
     * @param interruptions 최근 차단 이벤트 리스트
     * @return ComprehensiveInsights 객체
     */
    fun calculateComprehensiveInsights(
        sessions: List<FocusSession>,
        interruptions: List<FocusInterruption>
    ): ComprehensiveInsights {
        // 1. 기본 통계
        val basicStats = basicStatisticsCalculator.calculateSummary(sessions)

        // 2. 위험 지수
        val riskIndex = riskCalculator.calculateRiskIndex(
            sessions = sessions,
            totalInterruptions = interruptions.size
        )

        // 3. 회복률 추세 (7일 기준)
        val recoveryTrend = recoveryCalculator.calculateRecoveryTrend(
            sessions = sessions,
            periodDays = 7
        )

        // 4. 방해요인 Top 3
        val topDistractions = if (interruptions.isNotEmpty()) {
            interruptionAnalyzer.getTopDistractionCategories(interruptions, limit = 3)
        } else {
            emptyList()
        }

        // 5. 유혹 저항 시간 분석
        val resistanceAnalysis = interruptionAnalyzer.analyzeResistanceTime(sessions)

        // 6. 포기 지점 분석
        val giveUpAnalysis = interruptionAnalyzer.analyzeGiveUpPoint(sessions)

        // 7. 코치 추천
        val coachRecommendation = coachRecommender.generateRecommendation(
            riskIndex = riskIndex,
            recoveryTrend = recoveryTrend,
            topDistraction = topDistractions.firstOrNull()
        )

        return ComprehensiveInsights(
            basicStatistics = basicStats,
            riskIndex = riskIndex,
            recoveryTrend = recoveryTrend,
            topDistractions = topDistractions,
            resistanceAnalysis = resistanceAnalysis,
            giveUpAnalysis = giveUpAnalysis,
            coachRecommendation = coachRecommendation
        )
    }

    /**
     * 월별 종합 인사이트 계산 (최근 30일 기준)
     *
     * @param sessions 최근 30일 세션 리스트
     * @param interruptions 최근 30일 차단 이벤트 리스트
     * @return MonthlyInsights 객체
     */
    fun calculateMonthlyInsights(
        sessions: List<FocusSession>,
        interruptions: List<FocusInterruption>
    ): MonthlyInsights {
        // 1. 기본 통계
        val basicStats = basicStatisticsCalculator.calculateSummary(sessions)

        // 2. 위험 지수
        val riskIndex = riskCalculator.calculateRiskIndex(
            sessions = sessions,
            totalInterruptions = interruptions.size
        )

        // 3. 회복률 추세 (30일 기준)
        val recoveryTrend = recoveryCalculator.calculateRecoveryTrend(
            sessions = sessions,
            periodDays = 30
        )

        // 4. 월별 회복률 비교
        val monthlyComparison = recoveryCalculator.calculateMonthlyComparison(sessions)

        // 5. 방해요인 Top 3
        val topDistractions = if (interruptions.isNotEmpty()) {
            interruptionAnalyzer.getTopDistractionCategories(interruptions, limit = 3)
        } else {
            emptyList()
        }

        // 6. 세션당 평균 차단 이벤트 수
        val avgInterruptionsPerSession = interruptionAnalyzer.calculateAvgInterruptionsPerSession(
            sessions = sessions,
            interruptions = interruptions
        )

        return MonthlyInsights(
            basicStatistics = basicStats,
            riskIndex = riskIndex,
            recoveryTrend = recoveryTrend,
            monthlyComparison = monthlyComparison,
            topDistractions = topDistractions,
            avgInterruptionsPerSession = avgInterruptionsPerSession
        )
    }

    /**
     * 간단한 일일 요약
     *
     * @param sessions 오늘의 세션 리스트
     * @return DailySummary 객체
     */
    fun calculateDailySummary(sessions: List<FocusSession>): DailySummary {
        if (sessions.isEmpty()) {
            return DailySummary(
                totalSessions = 0,
                successfulSessions = 0,
                totalFocusTimeSeconds = 0,
                successRate = 0f,
                todayPoints = 0
            )
        }

        val successfulSessions = sessions.count { it.success }
        val totalFocusTime = basicStatisticsCalculator.calculateTotalFocusTime(sessions)
        val successRate = basicStatisticsCalculator.calculateFocusRate(sessions)
        val todayPoints = sessions.filter { it.success }.sumOf { it.durationMinutes }

        return DailySummary(
            totalSessions = sessions.size,
            successfulSessions = successfulSessions,
            totalFocusTimeSeconds = totalFocusTime,
            successRate = successRate,
            todayPoints = todayPoints
        )
    }
}

/**
 * 종합 인사이트 (7일 기준)
 */
data class ComprehensiveInsights(
    val basicStatistics: FocusStatisticsSummary,    // 기본 통계
    val riskIndex: DetoxyRiskIndex,                 // 위험 지수
    val recoveryTrend: DetoxyRecoveryTrend,         // 회복률 추세
    val topDistractions: List<DistractionItem>,     // 방해요인 Top 3
    val resistanceAnalysis: ResistanceTimeAnalysis, // 유혹 저항 시간
    val giveUpAnalysis: GiveUpPointAnalysis,        // 포기 지점 분석
    val coachRecommendation: CoachRecommendation    // 코치 추천
)

/**
 * 월별 인사이트 (30일 기준)
 */
data class MonthlyInsights(
    val basicStatistics: FocusStatisticsSummary,    // 기본 통계
    val riskIndex: DetoxyRiskIndex,                 // 위험 지수
    val recoveryTrend: DetoxyRecoveryTrend,         // 회복률 추세
    val monthlyComparison: DetoxyMonthlyComparison, // 월별 비교
    val topDistractions: List<DistractionItem>,     // 방해요인 Top 3
    val avgInterruptionsPerSession: Float           // 세션당 평균 차단 횟수
)

/**
 * 일일 요약
 */
data class DailySummary(
    val totalSessions: Int,         // 총 세션 수
    val successfulSessions: Int,    // 성공 세션 수
    val totalFocusTimeSeconds: Long,// 총 집중 시간 (초)
    val successRate: Float,         // 성공률 (%)
    val todayPoints: Int            // 오늘 획득 포인트
)

