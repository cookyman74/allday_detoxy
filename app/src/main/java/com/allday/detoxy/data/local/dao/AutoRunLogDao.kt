package com.allday.detoxy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.allday.detoxy.data.local.entity.AutoRunLog
import kotlinx.coroutines.flow.Flow

/**
 * AutoRunLog DAO (Data Access Object)
 *
 * 자동 실행 이력 데이터에 대한 데이터베이스 작업을 정의합니다.
 * Flow를 사용하여 반응형 데이터 스트림을 제공합니다.
 *
 * @see AutoRunLog
 */
@Dao
interface AutoRunLogDao {

    /**
     * 새로운 자동 실행 로그 추가
     *
     * @param log 추가할 로그
     */
    @Insert
    suspend fun insert(log: AutoRunLog)

    /**
     * 모든 자동 실행 로그 조회 (최신순)
     *
     * @return 로그 리스트 (Flow)
     */
    @Query("SELECT * FROM auto_run_log ORDER BY triggerTime DESC")
    fun getAll(): Flow<List<AutoRunLog>>

    /**
     * 트리거 타입별 로그 조회
     *
     * @param triggerType 트리거 타입 (TIME, LOCATION)
     * @return 로그 리스트 (Flow)
     */
    @Query("SELECT * FROM auto_run_log WHERE triggerType = :triggerType ORDER BY triggerTime DESC")
    fun getByTriggerType(triggerType: String): Flow<List<AutoRunLog>>

    /**
     * 최근 N개 로그 조회
     *
     * @param limit 조회할 개수 (기본 10개)
     * @return 로그 리스트 (Flow)
     */
    @Query("SELECT * FROM auto_run_log ORDER BY triggerTime DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 10): Flow<List<AutoRunLog>>

    /**
     * 특정 기간 로그 조회
     *
     * @param startTime 시작 시간 (timestamp)
     * @param endTime 종료 시간 (timestamp)
     * @return 로그 리스트 (Flow)
     */
    @Query("SELECT * FROM auto_run_log WHERE triggerTime >= :startTime AND triggerTime <= :endTime ORDER BY triggerTime DESC")
    fun getLogsInRange(startTime: Long, endTime: Long): Flow<List<AutoRunLog>>

    /**
     * 특정 소스 ID의 로그 조회
     *
     * @param sourceId 트리거 소스 ID
     * @return 로그 리스트 (Flow)
     */
    @Query("SELECT * FROM auto_run_log WHERE triggerSourceId = :sourceId ORDER BY triggerTime DESC")
    fun getBySourceId(sourceId: String): Flow<List<AutoRunLog>>

    /**
     * 총 자동 실행 횟수 조회
     *
     * @return 총 횟수
     */
    @Query("SELECT COUNT(*) FROM auto_run_log")
    suspend fun getTotalCount(): Int

    /**
     * 성공 횟수 조회
     *
     * @return 성공 횟수 (result == STARTED)
     */
    @Query("SELECT COUNT(*) FROM auto_run_log WHERE result = 'STARTED'")
    suspend fun getSuccessCount(): Int

    /**
     * 실패 횟수 조회
     *
     * @return 실패 횟수 (result == FAILED)
     */
    @Query("SELECT COUNT(*) FROM auto_run_log WHERE result = 'FAILED'")
    suspend fun getFailureCount(): Int

    /**
     * 건너뛴 횟수 조회
     *
     * @return 건너뛴 횟수 (result == SKIPPED)
     */
    @Query("SELECT COUNT(*) FROM auto_run_log WHERE result = 'SKIPPED'")
    suspend fun getSkippedCount(): Int

    /**
     * 성공률 계산 (0.0 ~ 1.0)
     *
     * @return 성공률
     */
    @Query("""
        SELECT 
            CASE 
                WHEN COUNT(*) = 0 THEN 0.0
                ELSE CAST(SUM(CASE WHEN result = 'STARTED' THEN 1 ELSE 0 END) AS REAL) / COUNT(*)
            END
        FROM auto_run_log
    """)
    suspend fun getSuccessRate(): Float

    /**
     * 특정 트리거 타입의 성공률 계산
     *
     * @param triggerType 트리거 타입 (TIME, LOCATION)
     * @return 성공률
     */
    @Query("""
        SELECT 
            CASE 
                WHEN COUNT(*) = 0 THEN 0.0
                ELSE CAST(SUM(CASE WHEN result = 'STARTED' THEN 1 ELSE 0 END) AS REAL) / COUNT(*)
            END
        FROM auto_run_log
        WHERE triggerType = :triggerType
    """)
    suspend fun getSuccessRateByType(triggerType: String): Float

    /**
     * 특정 소스 ID의 성공률 계산 (위치별 성공률)
     *
     * @param sourceId 트리거 소스 ID
     * @return 성공률
     */
    @Query("""
        SELECT 
            CASE 
                WHEN COUNT(*) = 0 THEN 0.0
                ELSE CAST(SUM(CASE WHEN result = 'STARTED' THEN 1 ELSE 0 END) AS REAL) / COUNT(*)
            END
        FROM auto_run_log
        WHERE triggerSourceId = :sourceId
    """)
    suspend fun getSuccessRateBySource(sourceId: String): Float

    /**
     * 오늘 자동 실행 횟수 조회
     *
     * @return 오늘 자동 실행 횟수
     */
    @Query("""
        SELECT COUNT(*) 
        FROM auto_run_log 
        WHERE DATE(triggerTime/1000, 'unixepoch', 'localtime') = DATE('now', 'localtime')
    """)
    suspend fun getTodayCount(): Int

    /**
     * 이번 주 자동 실행 횟수 조회
     *
     * @return 이번 주 자동 실행 횟수
     */
    @Query("""
        SELECT COUNT(*) 
        FROM auto_run_log 
        WHERE DATE(triggerTime/1000, 'unixepoch', 'localtime') >= DATE('now', 'localtime', 'weekday 0', '-7 days')
    """)
    suspend fun getWeekCount(): Int
}

