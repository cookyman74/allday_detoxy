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
     * DATE(startTime/1000, 'unixepoch')는 밀리초를 초로 변환 후 날짜 추출
     *
     * @return 오늘의 세션 리스트 (Flow)
     */
    @Query("SELECT * FROM focus_sessions WHERE DATE(startTime/1000, 'unixepoch') = DATE('now') ORDER BY startTime DESC")
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
}
