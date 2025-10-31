package com.allday.detoxy.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import kotlinx.coroutines.flow.Flow

/**
 * LocationBasedAutoRun DAO (Data Access Object)
 *
 * 위치 기반 자동 실행 데이터에 대한 데이터베이스 작업을 정의합니다.
 * Flow를 사용하여 반응형 데이터 스트림을 제공합니다.
 *
 * @see LocationBasedAutoRun
 */
@Dao
interface LocationBasedAutoRunDao {

    /**
     * 새로운 위치 기반 자동 실행 추가
     *
     * @param autoRun 추가할 자동 실행
     */
    @Insert
    suspend fun insert(autoRun: LocationBasedAutoRun)

    /**
     * 위치 기반 자동 실행 업데이트
     *
     * @param autoRun 업데이트할 자동 실행
     */
    @Update
    suspend fun update(autoRun: LocationBasedAutoRun)

    /**
     * 위치 기반 자동 실행 삭제
     *
     * @param autoRun 삭제할 자동 실행
     */
    @Delete
    suspend fun delete(autoRun: LocationBasedAutoRun)

    /**
     * ID로 위치 기반 자동 실행 삭제
     *
     * @param id 삭제할 자동 실행 ID
     */
    @Query("DELETE FROM location_based_auto_run WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * 모든 위치 기반 자동 실행 조회 (생성 시간순)
     *
     * @return 자동 실행 리스트 (Flow)
     */
    @Query("SELECT * FROM location_based_auto_run ORDER BY createdAt DESC")
    fun getAll(): Flow<List<LocationBasedAutoRun>>

    /**
     * ID로 위치 기반 자동 실행 조회
     *
     * @param id 자동 실행 ID
     * @return 자동 실행 (Flow)
     */
    @Query("SELECT * FROM location_based_auto_run WHERE id = :id")
    fun getById(id: String): Flow<LocationBasedAutoRun?>

    /**
     * 활성화된 위치 기반 자동 실행 조회
     *
     * @return 활성화된 자동 실행 리스트 (Flow)
     */
    @Query("SELECT * FROM location_based_auto_run WHERE isEnabled = 1 ORDER BY createdAt DESC")
    fun getEnabled(): Flow<List<LocationBasedAutoRun>>

    /**
     * 활성화/비활성화 토글
     *
     * @param id 자동 실행 ID
     * @param isEnabled 활성화 여부
     */
    @Query("UPDATE location_based_auto_run SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun toggleEnabled(id: String, isEnabled: Boolean)

    /**
     * 총 개수 조회
     *
     * @return 총 개수
     */
    @Query("SELECT COUNT(*) FROM location_based_auto_run")
    suspend fun getCount(): Int

    /**
     * 활성화된 자동 실행 개수 조회
     *
     * @return 활성화된 개수
     */
    @Query("SELECT COUNT(*) FROM location_based_auto_run WHERE isEnabled = 1")
    suspend fun getEnabledCount(): Int
    
    /**
     * 활성화된 모든 위치 기반 자동 실행 조회 (suspend 함수)
     *
     * BootCompletedReceiver에서 재등록 시 사용
     *
     * @return 활성화된 자동 실행 리스트
     */
    @Query("SELECT * FROM location_based_auto_run WHERE isEnabled = 1")
    suspend fun getAllEnabled(): List<LocationBasedAutoRun>
    
    /**
     * 활성화된 모든 위치 기반 자동 실행 ID 조회 (suspend 함수)
     *
     * removeAllGeofences() 등에서 사용
     *
     * @return 활성화된 자동 실행 ID 리스트
     */
    @Query("SELECT id FROM location_based_auto_run WHERE isEnabled = 1")
    suspend fun getAllEnabledIds(): List<String>
    
    /**
     * 특정 ID의 활성화 여부 확인
     *
     * 재등록 시 MAX_GEOFENCES 체크에서 자신을 제외하기 위해 사용
     *
     * @param id 확인할 자동 실행 ID
     * @return true: 활성화됨, false: 비활성화 또는 존재하지 않음
     */
    @Query("SELECT isEnabled FROM location_based_auto_run WHERE id = :id")
    suspend fun isEnabled(id: String): Boolean?
    
    /**
     * 특정 ID를 제외한 활성화된 자동 실행 개수 조회
     *
     * MAX_GEOFENCES 체크 시 자기 자신을 제외한 개수를 확인하기 위해 사용합니다.
     * 신규 등록과 재등록을 구분 없이 올바르게 처리할 수 있습니다.
     *
     * @param excludeId 제외할 자동 실행 ID
     * @return 해당 ID를 제외한 활성화된 개수
     */
    @Query("SELECT COUNT(*) FROM location_based_auto_run WHERE isEnabled = 1 AND id != :excludeId")
    suspend fun getEnabledCountExcept(excludeId: String): Int

    // ==================== v5 추가: ScheduleGroup 지원 ====================

    /**
     * 특정 스케줄 그룹에 연결된 위치 기반 자동 실행 조회 (v5+)
     *
     * @param scheduleGroupId 스케줄 그룹 ID
     * @return 해당 그룹에 연결된 LocationBasedAutoRun 리스트
     */
    @Query("SELECT * FROM location_based_auto_run WHERE linkedScheduleGroupId = :scheduleGroupId ORDER BY createdAt DESC")
    suspend fun getByLinkedGroup(scheduleGroupId: String): List<LocationBasedAutoRun>

    /**
     * 특정 스케줄 그룹에서 위치 참조 해제 (v5+)
     *
     * linkedScheduleGroupId를 NULL로 설정하여 단순 트리거 모드로 전환합니다.
     *
     * @param scheduleGroupId 스케줄 그룹 ID
     */
    @Query("UPDATE location_based_auto_run SET linkedScheduleGroupId = NULL WHERE linkedScheduleGroupId = :scheduleGroupId")
    suspend fun unlinkFromGroup(scheduleGroupId: String)

    // ==================== v6 추가: 위치-시간표 연동 조회 ====================

    /**
     * 진입 시 시간표 자동 활성화 옵션이 설정된 위치 조회 (v6+)
     *
     * activateScheduleOnEnter = true인 위치들만 조회합니다.
     * GeofenceTransitionsReceiver에서 위치 진입 시 시간표 활성화에 사용됩니다.
     *
     * @return 자동 활성화 옵션이 설정된 LocationBasedAutoRun 리스트 Flow
     */
    @Query("SELECT * FROM location_based_auto_run WHERE activateScheduleOnEnter = 1 AND isEnabled = 1")
    fun getAutoActivateLocations(): Flow<List<LocationBasedAutoRun>>

    /**
     * 이탈 시 시간표 자동 비활성화 옵션이 설정된 위치 조회 (v6+)
     *
     * deactivateScheduleOnExit = true인 위치들만 조회합니다.
     * GeofenceTransitionsReceiver에서 위치 이탈 시 시간표 비활성화에 사용됩니다.
     *
     * @return 자동 비활성화 옵션이 설정된 LocationBasedAutoRun 리스트 Flow
     */
    @Query("SELECT * FROM location_based_auto_run WHERE deactivateScheduleOnExit = 1 AND isEnabled = 1")
    fun getAutoDeactivateLocations(): Flow<List<LocationBasedAutoRun>>
}

