package com.allday.detoxy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.allday.detoxy.data.local.entity.FocusInterruption
import kotlinx.coroutines.flow.Flow

/**
 * 카테고리별 차단 횟수
 *
 * @property category 카테고리명
 * @property count 차단 횟수
 */
data class CategoryCount(
    val category: String,
    val count: Int
)

/**
 * FocusInterruption DAO (Data Access Object)
 *
 * 집중 세션 중 차단 이벤트 데이터에 대한 데이터베이스 작업을 정의합니다.
 * 세션별 차단 패턴 분석 및 카테고리별 통계를 제공합니다.
 */
@Dao
interface FocusInterruptionDao {

    /**
     * 차단 이벤트 추가
     *
     * @param interruption 추가할 차단 이벤트
     */
    @Insert
    suspend fun insert(interruption: FocusInterruption)

    /**
     * 특정 세션의 모든 차단 이벤트 조회
     *
     * @param sessionId 세션 ID
     * @return 차단 이벤트 리스트 (Flow)
     */
    @Query("SELECT * FROM focus_interruptions WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getInterruptionsBySession(sessionId: String): Flow<List<FocusInterruption>>

    /**
     * 특정 세션의 차단 이벤트 개수 조회
     *
     * @param sessionId 세션 ID
     * @return 차단 이벤트 개수
     */
    @Query("SELECT COUNT(*) FROM focus_interruptions WHERE sessionId = :sessionId")
    suspend fun getInterruptionCountBySession(sessionId: String): Int

    /**
     * 특정 세션의 카테고리별 차단 횟수 조회
     *
     * @param sessionId 세션 ID
     * @return 카테고리와 횟수 리스트
     */
    @Query("SELECT category, COUNT(*) as count FROM focus_interruptions WHERE sessionId = :sessionId GROUP BY category ORDER BY count DESC")
    suspend fun getCategoryCountsBySession(sessionId: String): List<CategoryCount>

    /**
     * 특정 세션의 가장 많이 차단된 카테고리 조회
     *
     * @param sessionId 세션 ID
     * @return 가장 많이 차단된 카테고리명 (없으면 null)
     */
    @Query("SELECT category FROM focus_interruptions WHERE sessionId = :sessionId GROUP BY category ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun getPrimaryCategoryBySession(sessionId: String): String?

    /**
     * 오늘 날짜의 모든 차단 이벤트 조회
     *
     * 'localtime' 변환을 추가하여 로컬 타임존(KST 등)에서 정확한 날짜 비교
     *
     * @return 오늘의 차단 이벤트 리스트 (Flow)
     */
    @Query("SELECT * FROM focus_interruptions WHERE DATE(timestamp/1000, 'unixepoch', 'localtime') = DATE('now', 'localtime') ORDER BY timestamp DESC")
    fun getTodayInterruptions(): Flow<List<FocusInterruption>>

    /**
     * 전체 차단 이벤트 조회 (최신순)
     *
     * @return 모든 차단 이벤트 리스트 (Flow)
     */
    @Query("SELECT * FROM focus_interruptions ORDER BY timestamp DESC")
    fun getAllInterruptions(): Flow<List<FocusInterruption>>

    /**
     * 특정 카테고리의 차단 이벤트 조회
     *
     * @param category 카테고리명
     * @return 해당 카테고리의 차단 이벤트 리스트 (Flow)
     */
    @Query("SELECT * FROM focus_interruptions WHERE category = :category ORDER BY timestamp DESC")
    fun getInterruptionsByCategory(category: String): Flow<List<FocusInterruption>>

    /**
     * 최근 N일 동안의 차단 이벤트 조회
     *
     * 'localtime' 변환을 사용하여 로컬 타임존 기준으로 날짜 계산
     *
     * @param days 조회할 일수 (예: 7, 30)
     * @return 최근 N일의 차단 이벤트 리스트
     *
     * Week 2B: Task 2B.3.1
     */
    @Query("""
        SELECT * FROM focus_interruptions 
        WHERE DATE(timestamp/1000, 'unixepoch', 'localtime') >= DATE('now', 'localtime', '-' || :days || ' days')
        ORDER BY timestamp DESC
    """)
    suspend fun getInterruptionsInLastDays(days: Int): List<FocusInterruption>
}

