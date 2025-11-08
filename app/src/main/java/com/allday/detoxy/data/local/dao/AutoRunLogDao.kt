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

    /**
     * 특정 기간 로그 조회 (suspend)
     *
     * 통계 계산용으로 Flow가 아닌 List를 반환합니다.
     *
     * @param startTime 시작 시간 (timestamp)
     * @param endTime 종료 시간 (timestamp)
     * @return 로그 리스트
     */
    @Query("SELECT * FROM auto_run_log WHERE triggerTime >= :startTime AND triggerTime <= :endTime ORDER BY triggerTime DESC")
    suspend fun getLogsInRangeList(startTime: Long, endTime: Long): List<AutoRunLog>

    /**
     * AutoRunLog 업데이트
     *
     * sessionId를 업데이트하거나 결과를 업데이트할 때 사용합니다.
     *
     * @param logId 업데이트할 로그 ID
     * @param sessionId 세션 ID (null 가능)
     * @param result 결과 (STARTED, FAILED, SKIPPED)
     * @param failureReason 실패 사유 (null 가능)
     */
    @Query("""
        UPDATE auto_run_log 
        SET sessionId = :sessionId, 
            result = :result, 
            failureReason = :failureReason
        WHERE id = :logId
    """)
    suspend fun update(
        logId: String,
        sessionId: String?,
        result: String,
        failureReason: String?
    )

    /**
     * 최근 로그 ID로 조회 (단일)
     *
     * 특정 triggerSourceId와 triggerTime으로 로그를 찾을 때 사용합니다.
     *
     * @param triggerSourceId 트리거 소스 ID
     * @param triggerTime 트리거 시간 (timestamp)
     * @return 로그 또는 null
     */
    @Query("SELECT * FROM auto_run_log WHERE triggerSourceId = :triggerSourceId AND triggerTime = :triggerTime LIMIT 1")
    suspend fun getBySourceAndTime(triggerSourceId: String, triggerTime: Long): AutoRunLog?

    /**
     * 위치 기반 자동 실행 로그 중 최근 STARTED 상태 로그 조회
     *
     * 특정 위치 ID들 중에서 가장 최근에 STARTED 상태로 기록된 로그를 찾습니다.
     * sessionId가 null인 로그만 대상으로 합니다 (타이머 시작 전 로그).
     * 최근 10분 내의 로그만 대상으로 합니다 (Geofence 진입 후 즉시 시작된 타이머에 연결).
     *
     * @param locationIds 위치 ID 리스트
     * @param currentTime 현재 시간 (timestamp, millis)
     * @return 가장 최근 STARTED 로그 또는 null
     */
    @Query("""
        SELECT * FROM auto_run_log 
        WHERE triggerType = 'LOCATION' 
          AND triggerSourceId IN (:locationIds)
          AND result = 'STARTED'
          AND sessionId IS NULL
          AND triggerTime >= :currentTime - 600000
        ORDER BY triggerTime DESC
        LIMIT 1
    """)
    suspend fun getRecentLocationStartedLog(locationIds: List<String>, currentTime: Long): AutoRunLog?
}

