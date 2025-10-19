package com.allday.detoxy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.allday.detoxy.data.local.entity.DetoxyRoutineLog
import kotlinx.coroutines.flow.Flow

/**
 * DetoxyRoutineLog DAO (Data Access Object)
 *
 * 디톡시 루틴 실행 기록 관리
 *
 * Week 2B: 루틴 달성률, 주간 진행도 계산용 쿼리 제공
 */
@Dao
interface DetoxyRoutineLogDao {

    /**
     * 새로운 루틴 기록 추가
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: DetoxyRoutineLog)

    /**
     * 오늘의 루틴 기록 조회
     */
    @Query("""
        SELECT * FROM detoxy_routine_logs 
        WHERE DATE(scheduledTime/1000, 'unixepoch', 'localtime') = DATE('now', 'localtime') 
        ORDER BY scheduledTime DESC
    """)
    fun getTodayRoutines(): Flow<List<DetoxyRoutineLog>>

    /**
     * 최근 N일 동안의 루틴 기록 조회
     */
    @Query("""
        SELECT * FROM detoxy_routine_logs 
        WHERE DATE(scheduledTime/1000, 'unixepoch', 'localtime') >= DATE('now', 'localtime', '-' || :days || ' days')
        ORDER BY scheduledTime DESC
    """)
    suspend fun getRoutinesInLastDays(days: Int): List<DetoxyRoutineLog>

    /**
     * 최근 N일 동안의 루틴 달성률 (성공/전체)
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as successful
        FROM detoxy_routine_logs 
        WHERE DATE(scheduledTime/1000, 'unixepoch', 'localtime') >= DATE('now', 'localtime', '-' || :days || ' days')
    """)
    suspend fun getRoutineSuccessRate(days: Int): RoutineSuccessStats?

    /**
     * 일별 루틴 성공 여부
     *
     * 주간 캘린더 뷰용
     */
    @Query("""
        SELECT 
            DATE(scheduledTime/1000, 'unixepoch', 'localtime') as date,
            SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) > 0 as has_success
        FROM detoxy_routine_logs 
        WHERE DATE(scheduledTime/1000, 'unixepoch', 'localtime') >= DATE('now', 'localtime', '-' || :days || ' days')
        GROUP BY date
        ORDER BY date DESC
    """)
    suspend fun getDailyRoutineStatus(days: Int = 7): List<DailyRoutineStatus>

    /**
     * 전체 루틴 기록 조회 (최신순)
     */
    @Query("SELECT * FROM detoxy_routine_logs ORDER BY scheduledTime DESC")
    fun getAllRoutines(): Flow<List<DetoxyRoutineLog>>

    /**
     * 성공한 루틴만 조회
     */
    @Query("SELECT * FROM detoxy_routine_logs WHERE success = 1 ORDER BY scheduledTime DESC")
    fun getSuccessfulRoutines(): Flow<List<DetoxyRoutineLog>>

    /**
     * 실패한 루틴만 조회
     */
    @Query("SELECT * FROM detoxy_routine_logs WHERE success = 0 ORDER BY scheduledTime DESC")
    fun getFailedRoutines(): Flow<List<DetoxyRoutineLog>>
}

/**
 * 루틴 성공 통계
 */
data class RoutineSuccessStats(
    val total: Int,
    val successful: Int
) {
    /**
     * 달성률 (%)
     */
    fun getSuccessRate(): Float {
        return if (total > 0) (successful.toFloat() / total) * 100 else 0f
    }
}

/**
 * 일별 루틴 상태
 */
data class DailyRoutineStatus(
    val date: String,       // YYYY-MM-DD
    val has_success: Boolean // 해당 날짜에 1개 이상 성공한 루틴이 있는지
)

