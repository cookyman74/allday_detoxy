package com.allday.detoxy.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.allday.detoxy.data.local.entity.CustomTimerPreset
import kotlinx.coroutines.flow.Flow

/**
 * CustomTimerPreset DAO (Data Access Object)
 *
 * 커스텀 타이머 프리셋 데이터에 대한 데이터베이스 작업을 정의합니다.
 * Flow를 사용하여 반응형 데이터 스트림을 제공합니다.
 *
 * @see CustomTimerPreset
 */
@Dao
interface CustomTimerPresetDao {

    /**
     * 새로운 커스텀 프리셋 추가
     *
     * @param preset 추가할 프리셋
     */
    @Insert
    suspend fun insert(preset: CustomTimerPreset)

    /**
     * 커스텀 프리셋 업데이트
     *
     * @param preset 업데이트할 프리셋
     */
    @Update
    suspend fun update(preset: CustomTimerPreset)

    /**
     * 커스텀 프리셋 삭제
     *
     * @param preset 삭제할 프리셋
     */
    @Delete
    suspend fun delete(preset: CustomTimerPreset)

    /**
     * ID로 커스텀 프리셋 삭제
     *
     * @param id 삭제할 프리셋 ID
     */
    @Query("DELETE FROM custom_timer_preset WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * 모든 커스텀 프리셋 조회 (표시 순서대로)
     *
     * @return 프리셋 리스트 (Flow)
     */
    @Query("SELECT * FROM custom_timer_preset ORDER BY displayOrder ASC")
    fun getAll(): Flow<List<CustomTimerPreset>>

    /**
     * ID로 커스텀 프리셋 조회
     *
     * @param id 프리셋 ID
     * @return 프리셋 (Flow)
     */
    @Query("SELECT * FROM custom_timer_preset WHERE id = :id")
    fun getById(id: String): Flow<CustomTimerPreset?>

    /**
     * 표시 순서대로 정렬된 프리셋 조회 (사용 횟수 포함)
     *
     * @return 프리셋 리스트 (Flow)
     */
    @Query("SELECT * FROM custom_timer_preset ORDER BY displayOrder ASC, usageCount DESC")
    fun getByDisplayOrder(): Flow<List<CustomTimerPreset>>

    /**
     * 표시 순서 업데이트
     *
     * @param id 프리셋 ID
     * @param displayOrder 새 표시 순서
     */
    @Query("UPDATE custom_timer_preset SET displayOrder = :displayOrder WHERE id = :id")
    suspend fun updateDisplayOrder(id: String, displayOrder: Int)

    /**
     * 사용 횟수 증가
     *
     * @param id 프리셋 ID
     */
    @Query("UPDATE custom_timer_preset SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsageCount(id: String)

    /**
     * 총 개수 조회
     *
     * @return 총 개수
     */
    @Query("SELECT COUNT(*) FROM custom_timer_preset")
    suspend fun getCount(): Int

    /**
     * 사용 횟수가 가장 많은 프리셋 조회
     *
     * @param limit 조회할 개수 (기본 3개)
     * @return 프리셋 리스트 (Flow)
     */
    @Query("SELECT * FROM custom_timer_preset ORDER BY usageCount DESC LIMIT :limit")
    fun getTopUsed(limit: Int = 3): Flow<List<CustomTimerPreset>>
}

