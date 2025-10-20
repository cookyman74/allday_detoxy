package com.allday.detoxy.domain.manager

import com.allday.detoxy.data.local.entity.FocusSession
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * DetoxyRiskCalculator 단위 테스트
 *
 * Week 2B: Task 2B.4 - 통합 테스트 & 품질
 */
class DetoxyRiskCalculatorTest {

    private lateinit var calculator: DetoxyRiskCalculator

    @Before
    fun setup() {
        calculator = DetoxyRiskCalculator()
    }

    @Test
    fun `빈 세션 리스트는 위험 지수 0을 반환한다`() {
        // Given
        val sessions = emptyList<FocusSession>()

        // When
        val riskIndex = calculator.calculateRiskIndex(sessions, totalInterruptions = 0)

        // Then
        assertEquals(0, riskIndex.score)
        assertEquals(RiskLevel.RECOVERY, riskIndex.level)
        assertEquals(0f, riskIndex.failureRate, 0.01f)
    }

    @Test
    fun `전체 성공 세션은 낮은 위험 지수를 반환한다`() {
        // Given: 10개 성공 세션
        val sessions = List(10) { index ->
            FocusSession(
                id = "session-${index + 1}",
                startTime = System.currentTimeMillis() - (index * 24 * 60 * 60 * 1000L),
                durationMinutes = 25,
                success = true,
                interruptedSeconds = 0,
                giveUpReason = null
            )
        }

        // When
        val riskIndex = calculator.calculateRiskIndex(sessions, totalInterruptions = 0)

        // Then
        assertTrue("성공 세션만 있으면 위험 지수가 낮아야 함", riskIndex.score <= 33)
        assertEquals(RiskLevel.RECOVERY, riskIndex.level)
        assertEquals(0f, riskIndex.failureRate, 0.01f)
    }

    @Test
    fun `전체 실패 세션은 높은 위험 지수를 반환한다`() {
        // Given: 10개 실패 세션 (모두 초반에 포기)
        val sessions = List(10) { index ->
            FocusSession(
                id = "session-${index + 1}",
                startTime = System.currentTimeMillis() - (index * 24 * 60 * 60 * 1000L),
                durationMinutes = 25,
                success = false,
                interruptedSeconds = 5 * 60, // 5분만에 포기
                giveUpReason = "distracted"
            )
        }

        // When
        val riskIndex = calculator.calculateRiskIndex(sessions, totalInterruptions = 50)

        // Then
        assertTrue("실패 세션만 있으면 위험 지수가 높아야 함", riskIndex.score >= 67)
        assertEquals(RiskLevel.HIGH_RISK, riskIndex.level)
        assertTrue("실패율이 높아야 함", riskIndex.failureRate >= 90f)
    }

    @Test
    fun `혼합 세션은 중간 위험 지수를 반환한다`() {
        // Given: 성공 5개, 실패 5개
        val sessions = mutableListOf<FocusSession>()
        
        // 성공 세션 5개
        repeat(5) { index ->
            sessions.add(
                FocusSession(
                    id = "session-${index + 1}",
                    startTime = System.currentTimeMillis() - (index * 24 * 60 * 60 * 1000L),
                    durationMinutes = 25,
                    success = true,
                    interruptedSeconds = 0,
                    giveUpReason = null
                )
            )
        }
        
        // 실패 세션 5개
        repeat(5) { index ->
            sessions.add(
                FocusSession(
                    id = "session-${index + 6}",
                    startTime = System.currentTimeMillis() - ((index + 5) * 24 * 60 * 60 * 1000L),
                    durationMinutes = 25,
                    success = false,
                    interruptedSeconds = 10 * 60, // 10분에 포기
                    giveUpReason = "distracted"
                )
            )
        }

        // When
        val riskIndex = calculator.calculateRiskIndex(sessions, totalInterruptions = 10)

        // Then
        assertTrue("혼합 세션은 중간 위험 지수여야 함", riskIndex.score in 20..80)
        assertEquals(50f, riskIndex.failureRate, 10f) // 실패율 50% 근처
    }

    @Test
    fun `연속 실패 세션은 패널티가 증가한다`() {
        // Given: 최근 5개 세션이 모두 실패 (연속 실패)
        val consecutiveFailSessions = List(5) { index ->
            FocusSession(
                id = "session-${index + 1}",
                startTime = System.currentTimeMillis() - (index * 24 * 60 * 60 * 1000L),
                durationMinutes = 25,
                success = false,
                interruptedSeconds = 5 * 60,
                giveUpReason = "distracted"
            )
        }

        // When
        val riskIndex = calculator.calculateRiskIndex(consecutiveFailSessions, totalInterruptions = 20)

        // Then
        assertTrue("연속 실패 패널티가 높아야 함", riskIndex.consecutiveFailsPenalty >= 50f)
    }

    @Test
    fun `포기 시점이 빠를수록 위험 지수가 높다`() {
        // Given: 초반 포기 vs 후반 포기
        val earlyGiveUpSessions = List(5) { index ->
            FocusSession(
                id = "session-early-${index + 1}",
                startTime = System.currentTimeMillis() - (index * 24 * 60 * 60 * 1000L),
                durationMinutes = 25,
                success = false,
                interruptedSeconds = 2 * 60, // 2분에 포기 (초반)
                giveUpReason = "distracted"
            )
        }

        val lateGiveUpSessions = List(5) { index ->
            FocusSession(
                id = "session-late-${index + 1}",
                startTime = System.currentTimeMillis() - (index * 24 * 60 * 60 * 1000L),
                durationMinutes = 25,
                success = false,
                interruptedSeconds = 20 * 60, // 20분에 포기 (후반)
                giveUpReason = "distracted"
            )
        }

        // When
        val earlyRisk = calculator.calculateRiskIndex(earlyGiveUpSessions, totalInterruptions = 10)
        val lateRisk = calculator.calculateRiskIndex(lateGiveUpSessions, totalInterruptions = 10)

        // Then
        assertTrue("초반 포기가 후반 포기보다 위험해야 함", earlyRisk.avgGiveUpTime > lateRisk.avgGiveUpTime)
    }

    @Test
    fun `차단 이벤트가 많을수록 위험 지수가 높다`() {
        // Given: 동일한 세션, 다른 차단 이벤트 수
        val sessions = List(10) { index ->
            FocusSession(
                id = "session-${index + 1}",
                startTime = System.currentTimeMillis() - (index * 24 * 60 * 60 * 1000L),
                durationMinutes = 25,
                success = false,
                interruptedSeconds = 10 * 60,
                giveUpReason = "distracted"
            )
        }

        // When
        val lowInterruptions = calculator.calculateRiskIndex(sessions, totalInterruptions = 5)
        val highInterruptions = calculator.calculateRiskIndex(sessions, totalInterruptions = 50)

        // Then
        assertTrue("차단 이벤트가 많으면 위험해야 함", 
            highInterruptions.interruptionFrequency > lowInterruptions.interruptionFrequency)
    }
}

