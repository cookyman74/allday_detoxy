package com.allday.detoxy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.allday.detoxy.data.local.entity.FocusDistraction
import kotlinx.coroutines.flow.Flow

/**
 * FocusDistraction DAO (Data Access Object)
 *
 * 집중 세션 중 감지된 모든 앱 진입 기록 (차단/허용 포함) 관리
 *
 * Week 2B: 고급 통계 계산을 위한 쿼리 제공
 * - 분산 회피율 계산
 * - 허용 앱 체류 시간 분석
 * - 카테고리별 주의 분산 패턴 분석
 */
@Dao
interface FocusDistractionDao {

    /**
     * 새로운 주의 분산 이벤트 추가
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(distraction: FocusDistraction)

    /**
     * 특정 세션의 모든 주의 분산 이벤트 조회
     */
    @Query("SELECT * FROM focus_distractions WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getBySession(sessionId: String): Flow<List<FocusDistraction>>

    /**
     * 특정 세션의 차단된 이벤트만 조회
     */
    @Query("SELECT * FROM focus_distractions WHERE sessionId = :sessionId AND wasBlocked = 1 ORDER BY timestamp ASC")
    fun getBlockedBySession(sessionId: String): Flow<List<FocusDistraction>>

    /**
     * 특정 세션의 허용된 이벤트만 조회
     */
    @Query("SELECT * FROM focus_distractions WHERE sessionId = :sessionId AND wasBlocked = 0 ORDER BY timestamp ASC")
    fun getAllowedBySession(sessionId: String): Flow<List<FocusDistraction>>

    /**
     * 오늘의 모든 주의 분산 이벤트 조회
     *
     * 'localtime' 변환을 사용하여 로컬 타임존 기준으로 날짜 계산
     */
    @Query("SELECT * FROM focus_distractions WHERE DATE(timestamp/1000, 'unixepoch', 'localtime') = DATE('now', 'localtime') ORDER BY timestamp DESC")
    fun getTodayDistractions(): Flow<List<FocusDistraction>>

    /**
     * 최근 N일 동안의 주의 분산 이벤트 조회
     */
    @Query("""
        SELECT * FROM focus_distractions 
        WHERE DATE(timestamp/1000, 'unixepoch', 'localtime') >= DATE('now', 'localtime', '-' || :days || ' days')
        ORDER BY timestamp DESC
    """)
    suspend fun getDistractionsInLastDays(days: Int): List<FocusDistraction>

    /**
     * 특정 세션의 총 허용 앱 체류 시간 (초)
     *
     * UsageStats 연동 시 사용
     */
    @Query("""
        SELECT SUM(dwellTimeSeconds) 
        FROM focus_distractions 
        WHERE sessionId = :sessionId 
        AND wasBlocked = 0 
        AND dwellTimeSeconds IS NOT NULL
    """)
    suspend fun getTotalAllowedDwellTime(sessionId: String): Int?

    /**
     * 카테고리별 주의 분산 횟수 (최근 N일)
     *
     * 방해요인 Top 3 분석용
     */
    @Query("""
        SELECT category, COUNT(*) as count 
        FROM focus_distractions 
        WHERE DATE(timestamp/1000, 'unixepoch', 'localtime') >= DATE('now', 'localtime', '-' || :days || ' days')
        GROUP BY category 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    suspend fun getTopDistractionCategories(days: Int = 7, limit: Int = 3): List<CategoryDistractionCount>

    /**
     * 분산 회피율 계산용: 5초 이내 이탈한 이벤트 비율
     *
     * @param sessionId 세션 ID
     * @return Pair<전체 이벤트 수, 5초 이내 이탈 이벤트 수>
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN dwellTimeSeconds <= 5 THEN 1 ELSE 0 END) as quick_exit
        FROM focus_distractions 
        WHERE sessionId = :sessionId 
        AND wasBlocked = 0 
        AND dwellTimeSeconds IS NOT NULL
    """)
    suspend fun getQuickExitStats(sessionId: String): QuickExitStats?

    /**
     * 전체 주의 분산 이벤트 조회 (최신순)
     */
    @Query("SELECT * FROM focus_distractions ORDER BY timestamp DESC")
    fun getAllDistractions(): Flow<List<FocusDistraction>>

    /**
     * 특정 카테고리의 주의 분산 이벤트 조회
     */
    @Query("SELECT * FROM focus_distractions WHERE category = :category ORDER BY timestamp DESC")
    fun getDistractionsByCategory(category: String): Flow<List<FocusDistraction>>
}

/**
 * 카테고리별 주의 분산 횟수
 */
data class CategoryDistractionCount(
    val category: String,
    val count: Int
)

/**
 * 분산 회피 통계
 */
data class QuickExitStats(
    val total: Int,
    val quick_exit: Int
)

