package com.allday.detoxy.domain.manager

/**
 * 자동 실행 통계 데이터
 *
 * 자동 실행 대시보드의 통계 카드에 표시할 데이터를 담는 데이터 클래스입니다.
 *
 * ## 주요 지표
 * - 이번 주 자동 실행 횟수 (시간/위치 분리)
 * - 자동 실행 성공률
 * - 자동 실행으로 얻은 총 집중 시간
 * - 주간 트렌드 (7일간 데이터)
 *
 * ## 사용 예시
 * ```kotlin
 * val statistics = AutoRunStatistics(
 *     weeklyTimeBasedCount = 15,
 *     weeklyLocationBasedCount = 8,
 *     successRate = 0.85f,
 *     totalFocusTimeMinutes = 450,
 *     weeklyTrend = listOf(...)
 * )
 * ```
 *
 * @property weeklyTimeBasedCount 이번 주 시간 기반 자동 실행 횟수
 * @property weeklyLocationBasedCount 이번 주 위치 기반 자동 실행 횟수
 * @property successRate 자동 실행 성공률 (0.0 ~ 1.0)
 * @property totalFocusTimeMinutes 자동 실행으로 얻은 총 집중 시간 (분)
 * @property weeklyTrend 주간 트렌드 데이터 (최근 7일)
 *
 * @see AutoRunStatisticsCalculator
 * @see DailyStats
 */
data class AutoRunStatistics(
    val weeklyTimeBasedCount: Int,
    val weeklyLocationBasedCount: Int,
    val successRate: Float, // 0.0 ~ 1.0
    val totalFocusTimeMinutes: Int,
    val weeklyTrend: List<DailyStats>
)

/**
 * 일별 통계 데이터
 *
 * 주간 트렌드 차트를 그리기 위한 일별 데이터를 담는 데이터 클래스입니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val dailyStats = DailyStats(
 *     date = "2025-10-28",
 *     timeBasedCount = 3,
 *     locationBasedCount = 1,
 *     successCount = 3,
 *     totalCount = 4
 * )
 * ```
 *
 * @property date 날짜 (YYYY-MM-DD 형식)
 * @property timeBasedCount 시간 기반 자동 실행 횟수
 * @property locationBasedCount 위치 기반 자동 실행 횟수
 * @property successCount 성공 횟수 (result == STARTED)
 * @property totalCount 전체 자동 실행 횟수
 */
data class DailyStats(
    val date: String, // YYYY-MM-DD
    val timeBasedCount: Int,
    val locationBasedCount: Int,
    val successCount: Int,
    val totalCount: Int
)

