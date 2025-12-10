package com.allday.detoxy.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.allday.detoxy.data.local.entity.ScheduleGroup
import kotlinx.coroutines.flow.Flow

/**
 * ScheduleGroup DAO (Data Access Object) (v5+)
 *
 * 스케줄 그룹 데이터에 대한 데이터베이스 작업을 정의합니다.
 * Flow를 사용하여 반응형 데이터 스트림을 제공합니다.
 *
 * @see ScheduleGroup
 */
@Dao
interface ScheduleGroupDao {

    /**
     * 새로운 스케줄 그룹 추가
     *
     * @param scheduleGroup 추가할 스케줄 그룹
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scheduleGroup: ScheduleGroup)

    /**
     * 스케줄 그룹 업데이트
     *
     * @param scheduleGroup 업데이트할 스케줄 그룹
     */
    @Update
    suspend fun update(scheduleGroup: ScheduleGroup)

    /**
     * 스케줄 그룹 삭제
     *
     * @param scheduleGroup 삭제할 스케줄 그룹
     */
    @Delete
    suspend fun delete(scheduleGroup: ScheduleGroup)

    /**
     * ID로 스케줄 그룹 삭제
     *
     * @param id 삭제할 스케줄 그룹 ID
     */
    @Query("DELETE FROM schedule_group WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * 모든 스케줄 그룹 조회 (생성 시간 역순)
     *
     * @return 스케줄 그룹 리스트 (Flow)
     */
    @Query("SELECT * FROM schedule_group ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ScheduleGroup>>

    /**
     * 활성화된 스케줄 그룹만 조회
     *
     * @return 활성화된 스케줄 그룹 리스트 (Flow)
     */
    @Query("SELECT * FROM schedule_group WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActive(): Flow<List<ScheduleGroup>>

    /**
     * ID로 스케줄 그룹 조회
     *
     * @param id 스케줄 그룹 ID
     * @return 스케줄 그룹 (Flow)
     */
    @Query("SELECT * FROM schedule_group WHERE id = :id")
    fun getById(id: String): Flow<ScheduleGroup?>

    /**
     * ID로 스케줄 그룹 조회 (suspend, 일회성)
     *
     * @param id 스케줄 그룹 ID
     * @return 스케줄 그룹 (nullable)
     */
    @Query("SELECT * FROM schedule_group WHERE id = :id")
    suspend fun getByIdOnce(id: String): ScheduleGroup?

    /**
     * 활성화/비활성화 토글
     *
     * @param id 스케줄 그룹 ID
     * @param isActive 활성화 여부
     */
    @Query("UPDATE schedule_group SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: String, isActive: Boolean)

    /**
     * 활성화/비활성화 및 마지막 활성화 시각 업데이트 (v6+)
     *
     * @param id 스케줄 그룹 ID
     * @param isActive 활성화 여부
     * @param timestamp 마지막 활성화 시각 (활성화 시 현재 시간, 비활성화 시 null)
     */
    @Query("UPDATE schedule_group SET isActive = :isActive, lastActivatedAt = :timestamp WHERE id = :id")
    suspend fun setActiveWithTimestamp(id: String, isActive: Boolean, timestamp: Long? = null)

    /**
     * 총 개수 조회
     *
     * @return 총 개수
     */
    @Query("SELECT COUNT(*) FROM schedule_group")
    suspend fun getCount(): Int

    /**
     * 활성화된 스케줄 그룹 개수 조회
     *
     * @return 활성화된 개수
     */
    @Query("SELECT COUNT(*) FROM schedule_group WHERE isActive = 1")
    suspend fun getActiveCount(): Int

    // ==================== v8 추가: 통합 제어 ====================

    /**
     * 수동 제어 상태 업데이트 (v8+)
     *
     * 스케줄 그룹의 활성/비활성/일시중지 상태를 사용자가 명시적으로 제어할 때 사용합니다.
     *
     * @param groupId 스케줄 그룹 ID
     * @param state 수동 제어 상태 (null: 자동 모드, "INACTIVE": 비활성화, "PAUSED": 일시중지)
     * @param pauseUntil 일시중지 해제 시각 (null: 일시중지 아님 또는 무기한)
     */
    @Query("UPDATE schedule_group SET manualOverrideState = :state, pauseUntil = :pauseUntil WHERE id = :groupId")
    suspend fun updateManualOverride(groupId: String, state: String?, pauseUntil: Long?)

    /**
     * 일시중지 만료된 그룹 자동 해제 (v8+)
     *
     * pauseUntil이 현재 시각보다 이전인 그룹의 manualOverrideState를 null로 초기화합니다.
     * 백그라운드 작업 또는 앱 시작 시 호출하여 만료된 일시중지 상태를 정리합니다.
     *
     * @param currentTime 현재 시각 (timestamp)
     * @return 업데이트된 행 수
     */
    @Query("""
        UPDATE schedule_group 
        SET manualOverrideState = NULL, pauseUntil = NULL 
        WHERE manualOverrideState = 'PAUSED' AND pauseUntil IS NOT NULL AND pauseUntil < :currentTime
    """)
    suspend fun clearExpiredPauses(currentTime: Long): Int
}

