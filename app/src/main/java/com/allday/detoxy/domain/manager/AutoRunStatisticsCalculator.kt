package com.allday.detoxy.domain.manager

import com.allday.detoxy.data.local.dao.AutoRunLogDao
import com.allday.detoxy.data.local.dao.FocusSessionDao
import com.allday.detoxy.data.local.entity.AutoRunLog
import com.allday.detoxy.data.local.entity.FocusSession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * 자동 실행 통계 계산기
 *
 * AutoRunLog와 FocusSession 데이터를 분석하여 통계를 생성합니다.
 *
 * ## 주요 기능
 * - 이번 주 자동 실행 횟수 계산 (시간/위치 분리)
 * - 자동 실행 성공률 계산
 * - 자동 실행으로 얻은 총 집중 시간 계산
 * - 주간 트렌드 생성 (최근 7일)
 *
 * ## 사용 예시
 * ```kotlin
 * val statistics = autoRunStatisticsCalculator.calculateStatistics()
 * ```
 *
 * @param autoRunLogDao AutoRunLog DAO
 * @param focusSessionDao FocusSession DAO
 *
 * @see AutoRunStatistics
 * @see DailyStats
 */
class AutoRunStatisticsCalculator @Inject constructor(
    private val autoRunLogDao: AutoRunLogDao,
    private val focusSessionDao: FocusSessionDao
) {
    
    /**
     * 자동 실행 통계 계산
     *
     * 이번 주 자동 실행 데이터를 분석하여 통계를 생성합니다.
     *
     * @return 자동 실행 통계 데이터
     */
    suspend fun calculateStatistics(): AutoRunStatistics {
        // 이번 주 시작 시간 (일요일 00:00:00)
        val weekStartTime = getWeekStartTime()
        val now = System.currentTimeMillis()
        
        // 이번 주 자동 실행 로그 조회
        val weeklyLogs = getLogsInCurrentWeek(weekStartTime, now)
        
        // 시간/위치 기반 자동 실행 횟수
        val timeBasedCount = weeklyLogs.count { it.triggerType == "TIME" }
        val locationBasedCount = weeklyLogs.count { it.triggerType == "LOCATION" }
        
        // 성공률 계산
        val successRate = if (weeklyLogs.isEmpty()) {
            0.0f
        } else {
            weeklyLogs.count { it.result == "STARTED" }.toFloat() / weeklyLogs.size
        }
        
        // 자동 실행으로 얻은 총 집중 시간
        val totalFocusTimeMinutes = calculateTotalFocusTime(weeklyLogs)
        
        // 주간 트렌드 생성 (최근 7일)
        val weeklyTrend = calculateWeeklyTrend(weeklyLogs)
        
        return AutoRunStatistics(
            weeklyTimeBasedCount = timeBasedCount,
            weeklyLocationBasedCount = locationBasedCount,
            successRate = successRate,
            totalFocusTimeMinutes = totalFocusTimeMinutes,
            weeklyTrend = weeklyTrend
        )
    }
    
    /**
     * 이번 주 시작 시간 계산 (일요일 00:00:00)
     *
     * @return 이번 주 시작 시간 (timestamp)
     */
    private fun getWeekStartTime(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 이번 주 자동 실행 로그 조회
     *
     * Room의 Flow를 사용하지 않고 직접 쿼리하는 suspend 함수입니다.
     * (통계 계산은 일회성이므로 Flow 불필요)
     *
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 자동 실행 로그 리스트
     */
    private suspend fun getLogsInCurrentWeek(startTime: Long, endTime: Long): List<AutoRunLog> {
        return autoRunLogDao.getLogsInRangeList(startTime, endTime)
    }
    
    /**
     * 자동 실행으로 얻은 총 집중 시간 계산
     *
     * AutoRunLog의 sessionId를 통해 FocusSession과 연계하여
     * 성공한 세션의 실제 집중 시간을 합산합니다.
     *
     * @param logs 자동 실행 로그 리스트
     * @return 총 집중 시간 (분)
     */
    private suspend fun calculateTotalFocusTime(logs: List<AutoRunLog>): Int {
        var totalMinutes = 0
        
        // 성공한 로그만 필터링
        val successfulLogs = logs.filter { it.result == "STARTED" && it.sessionId != null }
        
        // 각 세션의 집중 시간 합산
        for (log in successfulLogs) {
            val sessionId = log.sessionId ?: continue
            
            // FocusSession 조회
            val session = focusSessionDao.getSessionByIdSync(sessionId)
            if (session != null) {
                // 세션의 실제 집중 시간 계산 (durationMinutes 또는 startTime ~ endTime 차이)
                // FocusSession에 durationMinutes 필드가 있다고 가정
                // 없으면 (endTime - startTime) / 60000으로 계산
                val durationMinutes = if (session.endTime != null) {
                    ((session.endTime - session.startTime) / 60000).toInt()
                } else {
                    // 세션이 진행 중이거나 endTime이 없는 경우
                    0
                }
                totalMinutes += durationMinutes
            }
        }
        
        return totalMinutes
    }
    
    /**
     * 주간 트렌드 생성 (최근 7일)
     *
     * 날짜별로 그룹핑하여 일별 통계를 생성합니다.
     *
     * @param logs 자동 실행 로그 리스트
     * @return 일별 통계 리스트 (7일)
     */
    private fun calculateWeeklyTrend(logs: List<AutoRunLog>): List<DailyStats> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        
        // 최근 7일 날짜 생성
        val last7Days = (0 until 7).map { daysAgo ->
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
            dateFormat.format(calendar.time)
        }.reversed()
        
        // 날짜별로 로그 그룹핑
        val logsByDate = logs.groupBy { log ->
            dateFormat.format(log.triggerTime)
        }
        
        // 일별 통계 생성
        return last7Days.map { date ->
            val dailyLogs = logsByDate[date] ?: emptyList()
            
            DailyStats(
                date = date,
                timeBasedCount = dailyLogs.count { it.triggerType == "TIME" },
                locationBasedCount = dailyLogs.count { it.triggerType == "LOCATION" },
                successCount = dailyLogs.count { it.result == "STARTED" },
                totalCount = dailyLogs.size
            )
        }
    }
}

