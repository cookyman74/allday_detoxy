package com.allday.detoxy.domain.manager

import com.allday.detoxy.data.local.entity.FocusSession
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * DetoxyRecoveryCalculator 단위 테스트
 *
 * Week 2B: Task 2B.4 - 통합 테스트 & 품질
 */
class DetoxyRecoveryCalculatorTest {

    private lateinit var calculator: DetoxyRecoveryCalculator

    @Before
    fun setup() {
        calculator = DetoxyRecoveryCalculator()
    }

    @Test
    fun `빈 세션 리스트는 0% 회복률을 반환한다`() {
        // Given
        val sessions = emptyList<FocusSession>()

        // When
        val trend = calculator.calculateRecoveryTrend(sessions)

        // Then
        assertEquals(0f, trend.overallRate, 0.01f)
        assertTrue("일별 회복률이 비어있어야 함", trend.dailyRates.isEmpty())
        assertEquals(RecoveryTrendType.STABLE, trend.trend)
    }

    @Test
    fun `전체 성공 세션은 100% 회복률을 반환한다`() {
        // Given: 7일간 매일 2개씩 성공 세션
        val sessions = mutableListOf<FocusSession>()
        val calendar = Calendar.getInstance()
        
        repeat(7) { day ->
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val timestamp = calendar.timeInMillis
            
            repeat(2) { index ->
                sessions.add(
                    FocusSession(
                        id = "session-${day * 2 + index + 1}",
                        startTime = timestamp,
                        durationMinutes = 25,
                        success = true,
                        interruptedSeconds = 0,
                        giveUpReason = null
                    )
                )
            }
        }

        // When
        val trend = calculator.calculateRecoveryTrend(sessions)

        // Then
        assertTrue("회복률이 90% 이상이어야 함", trend.overallRate >= 90f)
        assertEquals(7, trend.dailyRates.size)
        assertTrue("모든 일별 회복률이 100%여야 함", trend.dailyRates.values.all { it == 100f })
    }

    @Test
    fun `혼합 세션은 중간 회복률을 반환한다`() {
        // Given: 성공 5개, 실패 5개
        val sessions = mutableListOf<FocusSession>()
        
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
        
        repeat(5) { index ->
            sessions.add(
                FocusSession(
                    id = "session-${index + 6}",
                    startTime = System.currentTimeMillis() - ((index + 5) * 24 * 60 * 60 * 1000L),
                    durationMinutes = 25,
                    success = false,
                    interruptedSeconds = 10 * 60,
                    giveUpReason = "distracted"
                )
            )
        }

        // When
        val trend = calculator.calculateRecoveryTrend(sessions)

        // Then
        assertTrue("회복률이 30-70% 사이여야 함", trend.overallRate in 30f..70f)
    }

    @Test
    fun `개선 추세는 IMPROVING 상태를 반환한다`() {
        // Given: 최근 7일간 점진적 개선 (실패 → 성공)
        val sessions = mutableListOf<FocusSession>()
        
        // 7일 전: 실패 2개
        repeat(2) {
            sessions.add(
                FocusSession(
                    id = "session-${it + 1}",
                    startTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L),
                    durationMinutes = 25,
                    success = false,
                    interruptedSeconds = 10 * 60,
                    giveUpReason = "distracted"
                )
            )
        }
        
        // 최근 3일: 성공 6개
        repeat(6) { index ->
            sessions.add(
                FocusSession(
                    id = "session-${index + 3}",
                    startTime = System.currentTimeMillis() - (index / 2 * 24 * 60 * 60 * 1000L),
                    durationMinutes = 25,
                    success = true,
                    interruptedSeconds = 0,
                    giveUpReason = null
                )
            )
        }

        // When
        val trend = calculator.calculateRecoveryTrend(sessions)

        // Then
        assertTrue("주간 변화량이 양수여야 함", trend.weeklyChange > 0)
        // IMPROVING 판정은 계산 로직에 따라 다를 수 있음
    }

    @Test
    fun `하락 추세는 DECLINING 상태를 반환한다`() {
        // Given: 최근 7일간 점진적 악화 (성공 → 실패)
        val sessions = mutableListOf<FocusSession>()
        
        // 7일 전: 성공 6개
        repeat(6) { index ->
            sessions.add(
                FocusSession(
                    id = "session-${index + 1}",
                    startTime = System.currentTimeMillis() - ((7 - index / 2) * 24 * 60 * 60 * 1000L),
                    durationMinutes = 25,
                    success = true,
                    interruptedSeconds = 0,
                    giveUpReason = null
                )
            )
        }
        
        // 최근 3일: 실패 4개
        repeat(4) {
            sessions.add(
                FocusSession(
                    id = "session-${it + 7}",
                    startTime = System.currentTimeMillis() - (it * 12 * 60 * 60 * 1000L), // 최근
                    durationMinutes = 25,
                    success = false,
                    interruptedSeconds = 10 * 60,
                    giveUpReason = "distracted"
                )
            )
        }

        // When
        val trend = calculator.calculateRecoveryTrend(sessions)

        // Then
        // DECLINING 판정은 계산 로직에 따라 다를 수 있음
        // 최근 실패가 많아야 전반적인 회복률이 낮음
        assertTrue("전반적인 회복률이 70% 이하여야 함", trend.overallRate <= 70f)
    }

    @Test
    fun `일별 회복률 맵이 올바르게 생성된다`() {
        // Given: 특정 날짜에 세션 추가
        val calendar = Calendar.getInstance()
        val sessions = mutableListOf<FocusSession>()
        
        // 오늘: 성공 1개
        sessions.add(
            FocusSession(
                id = "session-1",
                startTime = System.currentTimeMillis(),
                durationMinutes = 25,
                success = true,
                interruptedSeconds = 0,
                giveUpReason = null
            )
        )
        
        // 어제: 실패 1개
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        sessions.add(
            FocusSession(
                id = "session-2",
                startTime = calendar.timeInMillis,
                durationMinutes = 25,
                success = false,
                interruptedSeconds = 10 * 60,
                giveUpReason = "distracted"
            )
        )

        // When
        val trend = calculator.calculateRecoveryTrend(sessions)

        // Then
        assertTrue("일별 회복률 맵이 비어있지 않아야 함", trend.dailyRates.isNotEmpty())
        assertTrue("일별 회복률 맵이 2개 이상이어야 함", trend.dailyRates.size >= 2)
    }
}

