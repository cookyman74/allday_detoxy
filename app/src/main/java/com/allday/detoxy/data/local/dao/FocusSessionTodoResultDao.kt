package com.allday.detoxy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.allday.detoxy.data.local.entity.FocusSessionTodoResultEntity
import kotlinx.coroutines.flow.Flow

/**
 * 집중 세션 할일 결과 DAO
 */
@Dao
interface FocusSessionTodoResultDao {
    
    /**
     * 세션 ID로 할일 결과 조회
     */
    @Query("SELECT * FROM focus_session_todo_result WHERE sessionId = :sessionId")
    suspend fun getBySessionId(sessionId: String): FocusSessionTodoResultEntity?
    
    /**
     * 스케줄 ID로 모든 할일 결과 조회
     */
    @Query("SELECT * FROM focus_session_todo_result WHERE scheduleId = :scheduleId ORDER BY completedAt DESC")
    suspend fun getByScheduleId(scheduleId: String): List<FocusSessionTodoResultEntity>
    
    /**
     * 모든 할일 결과 조회 (최신순)
     */
    @Query("SELECT * FROM focus_session_todo_result ORDER BY completedAt DESC")
    fun getAllFlow(): Flow<List<FocusSessionTodoResultEntity>>
    
    /**
     * 수정 필요 항목 조회 (NO_RESPONSE 또는 NOT_COMPLETED 포함)
     * 
     * ## 쿼리 방식
     * todoResultsJson에 "status":"NO_RESPONSE" 또는 "status":"NOT_COMPLETED" 패턴이 포함된 결과
     * 
     * ## 주의사항
     * JSON LIKE 쿼리는 내용(content) 필드에 해당 문자열이 포함된 경우에도 매칭될 수 있음.
     * 정확한 필터링이 필요하면 Repository 계층에서 JSON 파싱 후 재필터링 권장.
     */
    @Query("""
        SELECT * FROM focus_session_todo_result 
        WHERE todoResultsJson LIKE '%"status":"NO_RESPONSE"%' 
           OR todoResultsJson LIKE '%"status":"NOT_COMPLETED"%'
        ORDER BY completedAt DESC
    """)
    fun getModifiableResultsFlow(): Flow<List<FocusSessionTodoResultEntity>>
    
    /**
     * 할일 결과 저장/업데이트
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: FocusSessionTodoResultEntity)
    
    /**
     * 할일 결과 JSON 및 수정 시각 업데이트
     */
    @Query("""
        UPDATE focus_session_todo_result 
        SET todoResultsJson = :json, lastModifiedAt = :time 
        WHERE sessionId = :sessionId
    """)
    suspend fun updateResults(sessionId: String, json: String, time: Long)
    
    /**
     * 기간 내 할일 결과 조회
     */
    @Query("""
        SELECT * FROM focus_session_todo_result 
        WHERE completedAt BETWEEN :startTime AND :endTime
        ORDER BY completedAt DESC
    """)
    suspend fun getByDateRange(startTime: Long, endTime: Long): List<FocusSessionTodoResultEntity>
}
