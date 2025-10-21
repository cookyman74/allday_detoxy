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
}

