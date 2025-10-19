package com.allday.detoxy.domain.manager

import com.allday.detoxy.data.local.entity.FocusSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * 디톡시 위험 지수(Risk Index) 계산기
 *
 * 차단/허용 이벤트, 사용 시간, 연속 실패 패턴을 가중치로 산출한 0~100 스코어
 *
 * ## 위험 단계
 * - **recovery** (0-33): 회복 단계, 안정적인 디톡시 진행
 * - **warning** (34-66): 주의 단계, 주의가 필요한 상태
 * - **high_risk** (67-100): 고위험 단계, 즉각적인 개입 필요
 *
 * ## 산식 구성 요소
 * 1. **실패율 (40%)**: 최근 세션의 실패 비율
 * 2. **연속 실패 패널티 (25%)**: 연속 실패 세션에 대한 가중치
 * 3. **포기 시점 (20%)**: 세션 시작 후 얼마나 빨리 포기했는지
 * 4. **차단 이벤트 빈도 (15%)**: 차단 시도가 많을수록 위험
 *
 * Week 2B: Task 2B.2
 */
@Singleton
class DetoxyRiskCalculator @Inject constructor() {

    companion object {
        // 가중치
        private const val WEIGHT_FAILURE_RATE = 0.40f       // 실패율
        private const val WEIGHT_CONSECUTIVE_FAILS = 0.25f  // 연속 실패
        private const val WEIGHT_GIVE_UP_TIME = 0.20f       // 포기 시점
        private const val WEIGHT_INTERRUPTION_FREQ = 0.15f  // 차단 빈도

        // 위험 지수 임계값
        private const val THRESHOLD_RECOVERY = 33
        private const val THRESHOLD_WARNING = 66
    }

    /**
     * 위험 지수 계산 (0-100)
     *
     * @param sessions 분석할 FocusSession 리스트 (최근 7~30일)
     * @param totalInterruptions 총 차단 이벤트 수
     * @return DetoxyRiskIndex 객체
     */
    fun calculateRiskIndex(
        sessions: List<FocusSession>,
        totalInterruptions: Int = 0
    ): DetoxyRiskIndex {
        if (sessions.isEmpty()) {
            return DetoxyRiskIndex(
                score = 0,
                level = RiskLevel.RECOVERY,
                failureRate = 0f,
                consecutiveFailsPenalty = 0f,
                avgGiveUpTime = 0f,
                interruptionFrequency = 0f
            )
        }

        // 1. 실패율 (0-100)
        val failureRate = calculateFailureRate(sessions)

        // 2. 연속 실패 패널티 (0-100)
        val consecutiveFailsPenalty = calculateConsecutiveFailsPenalty(sessions)

        // 3. 평균 포기 시점 (0-100, 빨리 포기할수록 높음)
        val avgGiveUpTime = calculateAverageGiveUpTime(sessions)

        // 4. 차단 이벤트 빈도 (0-100)
        val interruptionFrequency = calculateInterruptionFrequency(sessions.size, totalInterruptions)

        // 가중 평균으로 최종 스코어 계산
        val score = (
            failureRate * WEIGHT_FAILURE_RATE +
            consecutiveFailsPenalty * WEIGHT_CONSECUTIVE_FAILS +
            avgGiveUpTime * WEIGHT_GIVE_UP_TIME +
            interruptionFrequency * WEIGHT_INTERRUPTION_FREQ
        ).toInt()

        val level = when {
            score <= THRESHOLD_RECOVERY -> RiskLevel.RECOVERY
            score <= THRESHOLD_WARNING -> RiskLevel.WARNING
            else -> RiskLevel.HIGH_RISK
        }

        return DetoxyRiskIndex(
            score = score,
            level = level,
            failureRate = failureRate,
            consecutiveFailsPenalty = consecutiveFailsPenalty.toFloat(),
            avgGiveUpTime = avgGiveUpTime,
            interruptionFrequency = interruptionFrequency
        )
    }

    /**
     * 실패율 계산 (0-100)
     */
    private fun calculateFailureRate(sessions: List<FocusSession>): Float {
        val failedCount = sessions.count { !it.success }
        return (failedCount.toFloat() / sessions.size) * 100
    }

    /**
     * 연속 실패 패널티 계산 (0-100)
     *
     * 연속 실패가 많을수록 높은 점수
     */
    private fun calculateConsecutiveFailsPenalty(sessions: List<FocusSession>): Int {
        val sortedSessions = sessions.sortedBy { it.startTime }
        var maxConsecutiveFails = 0
        var currentConsecutiveFails = 0

        for (session in sortedSessions) {
            if (!session.success) {
                currentConsecutiveFails++
                maxConsecutiveFails = maxOf(maxConsecutiveFails, currentConsecutiveFails)
            } else {
                currentConsecutiveFails = 0
            }
        }

        // 연속 5회 실패 시 100점, 그 이하는 비례
        return min((maxConsecutiveFails * 20), 100)
    }

    /**
     * 평균 포기 시점 계산 (0-100)
     *
     * 실패 세션의 평균 지속 시간이 짧을수록 높은 점수
     * (빨리 포기할수록 위험)
     */
    private fun calculateAverageGiveUpTime(sessions: List<FocusSession>): Float {
        val failedSessions = sessions.filter { !it.success && it.interruptedSeconds > 0 }
        if (failedSessions.isEmpty()) return 0f

        val avgInterruptedSeconds = failedSessions.map { it.interruptedSeconds }.average()

        // 60초 이내 포기: 100점, 30분(1800초) 이후 포기: 0점
        val score = 100 - (avgInterruptedSeconds / 1800.0 * 100).toFloat()
        return score.coerceIn(0f, 100f)
    }

    /**
     * 차단 이벤트 빈도 계산 (0-100)
     *
     * 세션당 차단 이벤트 수가 많을수록 높은 점수
     */
    private fun calculateInterruptionFrequency(sessionCount: Int, totalInterruptions: Int): Float {
        if (sessionCount == 0) return 0f

        val avgInterruptionsPerSession = totalInterruptions.toFloat() / sessionCount

        // 세션당 10회 이상 차단 시 100점
        val score = (avgInterruptionsPerSession / 10.0 * 100).toFloat()
        return score.coerceIn(0f, 100f)
    }
}

/**
 * 디톡시 위험 지수 결과
 */
data class DetoxyRiskIndex(
    val score: Int,                     // 0-100 스코어
    val level: RiskLevel,               // 위험 단계
    val failureRate: Float,             // 실패율 (%)
    val consecutiveFailsPenalty: Float, // 연속 실패 패널티
    val avgGiveUpTime: Float,           // 평균 포기 시점 점수
    val interruptionFrequency: Float    // 차단 이벤트 빈도 점수
)

// RiskLevel enum은 별도 파일로 분리됨 (RiskLevel.kt)
// Week 2B: Task 2B.3.4

