package com.allday.detoxy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.allday.detoxy.data.local.entity.FocusSession
import kotlinx.coroutines.flow.Flow

/**
 * FocusSession DAO (Data Access Object)
 *
 * 집중 세션 데이터에 대한 데이터베이스 작업을 정의합니다.
 * Flow를 사용하여 반응형 데이터 스트림을 제공합니다.
 */
@Dao
interface FocusSessionDao {

    /**
     * 새로운 세션 추가
     *
     * @param session 추가할 세션
     */
    @Insert
    suspend fun insert(session: FocusSession)

    /**
     * 세션 업데이트
     *
     * @param session 업데이트할 세션
     */
    @Update
    suspend fun update(session: FocusSession)

    /**
     * 오늘 날짜의 모든 세션 조회
     *
     * Unix timestamp를 날짜로 변환하여 오늘과 비교합니다.
     * 'localtime' 변환을 추가하여 로컬 타임존(KST 등)에서 정확한 날짜 비교
     *
     * @return 오늘의 세션 리스트 (Flow)
     */
    @Query("SELECT * FROM focus_sessions WHERE DATE(startTime/1000, 'unixepoch', 'localtime') = DATE('now', 'localtime') ORDER BY startTime DESC")
    fun getTodaySessions(): Flow<List<FocusSession>>

    /**
     * 모든 세션 조회 (최신순)
     *
     * @return 모든 세션 리스트 (Flow)
     */
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    /**
     * 특정 ID의 세션 조회
     *
     * @param sessionId 세션 ID
     * @return 세션 (Flow)
     */
    @Query("SELECT * FROM focus_sessions WHERE id = :sessionId")
    fun getSessionById(sessionId: String): Flow<FocusSession?>

    /**
     * 성공한 세션만 조회
     *
     * @return 성공한 세션 리스트 (Flow)
     */
    @Query("SELECT * FROM focus_sessions WHERE success = 1 ORDER BY startTime DESC")
    fun getSuccessfulSessions(): Flow<List<FocusSession>>

    /**
     * 최근 N일 동안의 세션 조회
     *
     * 'localtime' 변환을 사용하여 로컬 타임존 기준으로 날짜 계산
     *
     * @param days 조회할 일수 (예: 7, 30)
     * @return 최근 N일의 세션 리스트
     */
    @Query("""
        SELECT * FROM focus_sessions 
        WHERE DATE(startTime/1000, 'unixepoch', 'localtime') >= DATE('now', 'localtime', '-' || :days || ' days')
        ORDER BY startTime DESC
    """)
    suspend fun getSessionsInLastDays(days: Int): List<FocusSession>

    /**
     * 특정 날짜 범위의 세션 조회
     *
     * @param startTimestamp 시작 시간 (Unix timestamp, milliseconds)
     * @param endTimestamp 종료 시간 (Unix timestamp, milliseconds)
     * @return 범위 내 세션 리스트
     */
    @Query("""
        SELECT * FROM focus_sessions 
        WHERE startTime >= :startTimestamp AND startTime <= :endTimestamp
        ORDER BY startTime DESC
    """)
    suspend fun getSessionsInRange(startTimestamp: Long, endTimestamp: Long): List<FocusSession>

    /**
     * 특정 ID의 세션 조회 (suspend)
     *
     * 통계 계산용으로 Flow가 아닌 nullable FocusSession을 반환합니다.
     *
     * @param sessionId 세션 ID
     * @return 세션 (nullable)
     */
    @Query("SELECT * FROM focus_sessions WHERE id = :sessionId")
    suspend fun getSessionByIdSync(sessionId: String): FocusSession?
}
