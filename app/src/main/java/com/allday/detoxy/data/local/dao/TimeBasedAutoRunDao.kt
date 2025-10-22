package com.allday.detoxy.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import kotlinx.coroutines.flow.Flow

/**
 * TimeBasedAutoRun DAO (Data Access Object)
 *
 * 시간 기반 자동 실행 데이터에 대한 데이터베이스 작업을 정의합니다.
 * Flow를 사용하여 반응형 데이터 스트림을 제공합니다.
 *
 * @see TimeBasedAutoRun
 */
@Dao
interface TimeBasedAutoRunDao {

    /**
     * 새로운 시간 기반 자동 실행 추가
     *
     * @param autoRun 추가할 자동 실행
     */
    @Insert
    suspend fun insert(autoRun: TimeBasedAutoRun)

    /**
     * 시간 기반 자동 실행 업데이트
     *
     * @param autoRun 업데이트할 자동 실행
     */
    @Update
    suspend fun update(autoRun: TimeBasedAutoRun)

    /**
     * 시간 기반 자동 실행 삭제
     *
     * @param autoRun 삭제할 자동 실행
     */
    @Delete
    suspend fun delete(autoRun: TimeBasedAutoRun)

    /**
     * ID로 시간 기반 자동 실행 삭제
     *
     * @param id 삭제할 자동 실행 ID
     */
    @Query("DELETE FROM time_based_auto_run WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * 모든 시간 기반 자동 실행 조회 (생성 시간순)
     *
     * @return 자동 실행 리스트 (Flow)
     */
    @Query("SELECT * FROM time_based_auto_run ORDER BY hour ASC, minute ASC")
    fun getAll(): Flow<List<TimeBasedAutoRun>>

    /**
     * ID로 시간 기반 자동 실행 조회
     *
     * @param id 자동 실행 ID
     * @return 자동 실행 (Flow)
     */
    @Query("SELECT * FROM time_based_auto_run WHERE id = :id")
    fun getById(id: String): Flow<TimeBasedAutoRun?>

    /**
     * 활성화된 시간 기반 자동 실행 조회
     *
     * @return 활성화된 자동 실행 리스트 (Flow)
     */
    @Query("SELECT * FROM time_based_auto_run WHERE isEnabled = 1 ORDER BY hour ASC, minute ASC")
    fun getEnabled(): Flow<List<TimeBasedAutoRun>>

    /**
     * 특정 요일에 활성화된 자동 실행 조회 (효율적인 스케줄링)
     *
     * enabledDays JSON에 특정 요일이 포함되어 있고 활성화된 자동 실행을 조회합니다.
     * 예: "MON"이 포함된 경우, ["MON","WED"] 또는 ["MON"] 등이 매칭됩니다.
     *
     * @param dayOfWeek 요일 코드 (예: "MON", "TUE", "WED", ...)
     * @return 해당 요일에 활성화된 자동 실행 리스트 (Flow)
     */
    @Query("""
        SELECT * FROM time_based_auto_run 
        WHERE isEnabled = 1 
        AND enabledDays LIKE '%' || :dayOfWeek || '%'
        ORDER BY hour ASC, minute ASC
    """)
    fun getEnabledForDay(dayOfWeek: String): Flow<List<TimeBasedAutoRun>>

    /**
     * 특정 시간대의 자동 실행 조회
     *
     * @param hour 시간 (0-23)
     * @param minute 분 (0-59)
     * @return 자동 실행 (Flow)
     */
    @Query("SELECT * FROM time_based_auto_run WHERE hour = :hour AND minute = :minute")
    fun getByTime(hour: Int, minute: Int): Flow<List<TimeBasedAutoRun>>

    /**
     * 활성화/비활성화 토글
     *
     * @param id 자동 실행 ID
     * @param isEnabled 활성화 여부
     */
    @Query("UPDATE time_based_auto_run SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun toggleEnabled(id: String, isEnabled: Boolean)

    /**
     * 총 개수 조회
     *
     * @return 총 개수
     */
    @Query("SELECT COUNT(*) FROM time_based_auto_run")
    suspend fun getCount(): Int

    /**
     * 활성화된 자동 실행 개수 조회
     *
     * @return 활성화된 개수
     */
    @Query("SELECT COUNT(*) FROM time_based_auto_run WHERE isEnabled = 1")
    suspend fun getEnabledCount(): Int
}

