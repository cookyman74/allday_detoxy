package com.allday.detoxy.domain.manager

import com.allday.detoxy.data.local.entity.FocusSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 집중 세션 통계 계산 유틸리티
 *
 * Task 2A.3: 기본 통계 계산 모듈
 *
 * 주요 기능:
 * - 총 집중 시간 계산 (성공 + 실패 세션 포함)
 * - 집중률 계산 (성공 세션 / 전체 세션)
 * - 평균 집중 유지 시간 계산 (성공/실패 분리)
 * - 포인트 누적 추세 계산 (7일/30일 기준)
 *
 * @see FocusSession
 */
@Singleton
class FocusStatisticsCalculator @Inject constructor() {

    /**
     * 총 집중 시간 계산 (초 단위)
     *
     * - 성공 세션: durationMinutes * 60
     * - 실패 세션: interruptedSeconds (실제 경과 시간)
     *
     * @param sessions 세션 리스트
     * @return 총 집중 시간 (초)
     */
    fun calculateTotalFocusTime(sessions: List<FocusSession>): Long {
        return sessions.sumOf { session ->
            if (session.success) {
                // 성공 세션: 전체 시간
                session.durationMinutes * 60L
            } else {
                // 실패 세션: 실제 경과 시간
                session.interruptedSeconds.toLong()
            }
        }
    }

    /**
     * 집중률 계산 (%)
     *
     * 집중률 = (성공 세션 수 / 전체 세션 수) * 100
     *
     * @param sessions 세션 리스트
     * @return 집중률 (0~100, 세션이 없으면 0.0)
     */
    fun calculateFocusRate(sessions: List<FocusSession>): Float {
        if (sessions.isEmpty()) return 0f

        val successCount = sessions.count { it.success }
        return (successCount.toFloat() / sessions.size) * 100
    }

    /**
     * 평균 집중 유지 시간 계산 (초 단위)
     *
     * 성공 세션과 실패 세션을 분리하여 각각의 평균 시간 계산
     *
     * @param sessions 세션 리스트
     * @return Pair<성공 세션 평균, 실패 세션 평균> (초 단위, 세션이 없으면 0)
     */
    fun calculateAverageFocusDuration(sessions: List<FocusSession>): Pair<Long, Long> {
        val successSessions = sessions.filter { it.success }
        val failedSessions = sessions.filter { !it.success }

        val avgSuccess = if (successSessions.isNotEmpty()) {
            successSessions.sumOf { it.durationMinutes * 60L } / successSessions.size
        } else {
            0L
        }

        val avgFailed = if (failedSessions.isNotEmpty()) {
            failedSessions.sumOf { it.interruptedSeconds.toLong() } / failedSessions.size
        } else {
            0L
        }

        return Pair(avgSuccess, avgFailed)
    }

    /**
     * 포인트 누적 추세 계산 (일별)
     *
     * 7일 또는 30일 기준으로 일별 포인트 합산
     * - 성공 세션만 포인트 획득 (1분 = 1포인트)
     *
     * @param sessions 세션 리스트 (최근 N일)
     * @return Map<날짜 (YYYY-MM-DD), 포인트>
     */
    fun calculateDailyPointsTrend(sessions: List<FocusSession>): Map<String, Int> {
        return sessions
            .filter { it.success }  // 성공 세션만
            .groupBy { session ->
                // Unix timestamp를 날짜로 변환 (YYYY-MM-DD)
                val date = java.time.Instant.ofEpochMilli(session.startTime)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .toString()
                date
            }
            .mapValues { (_, sessionsInDay) ->
                // 일별 포인트 합산 (1분 = 1포인트)
                sessionsInDay.sumOf { it.durationMinutes }
            }
            .toSortedMap()  // 날짜 순으로 정렬
    }

    /**
     * 통합 통계 계산
     *
     * 모든 기본 통계를 한 번에 계산하여 반환
     *
     * @param sessions 세션 리스트
     * @return 통계 요약 객체
     */
    fun calculateSummary(sessions: List<FocusSession>): FocusStatisticsSummary {
        val (avgSuccess, avgFailed) = calculateAverageFocusDuration(sessions)

        return FocusStatisticsSummary(
            totalFocusTimeSeconds = calculateTotalFocusTime(sessions),
            focusRatePercent = calculateFocusRate(sessions),
            avgSuccessDurationSeconds = avgSuccess,
            avgFailedDurationSeconds = avgFailed,
            totalSessionsCount = sessions.size,
            successSessionsCount = sessions.count { it.success },
            failedSessionsCount = sessions.count { !it.success },
            dailyPointsTrend = calculateDailyPointsTrend(sessions)
        )
    }
}

/**
 * 통계 요약 데이터 클래스
 */
data class FocusStatisticsSummary(
    val totalFocusTimeSeconds: Long,      // 총 집중 시간 (초)
    val focusRatePercent: Float,          // 집중률 (%)
    val avgSuccessDurationSeconds: Long,  // 평균 성공 세션 시간 (초)
    val avgFailedDurationSeconds: Long,   // 평균 실패 세션 시간 (초)
    val totalSessionsCount: Int,          // 전체 세션 수
    val successSessionsCount: Int,        // 성공 세션 수
    val failedSessionsCount: Int,         // 실패 세션 수
    val dailyPointsTrend: Map<String, Int> // 일별 포인트 추세
)

