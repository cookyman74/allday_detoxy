package com.allday.detoxy.data.repository

import com.allday.detoxy.data.local.dao.CustomTimerPresetDao
import com.allday.detoxy.data.local.entity.CustomTimerPreset
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CustomTimerPreset Repository
 *
 * 커스텀 타이머 프리셋 데이터를 관리합니다.
 * DAO를 통해 Room 데이터베이스와 통신합니다.
 *
 * ## 주요 기능
 * - 커스텀 프리셋 CRUD
 * - 표시 순서 관리
 * - 사용 횟수 자동 증가
 *
 * @property dao CustomTimerPresetDao
 */
@Singleton
class CustomTimerPresetRepository @Inject constructor(
    private val dao: CustomTimerPresetDao
) {

    /**
     * 모든 커스텀 프리셋 조회 (표시 순서대로)
     *
     * @return 프리셋 리스트 (Flow)
     */
    fun getAll(): Flow<List<CustomTimerPreset>> {
        return dao.getAll()
    }

    /**
     * ID로 커스텀 프리셋 조회
     *
     * @param id 프리셋 ID
     * @return 프리셋 (Flow)
     */
    fun getById(id: String): Flow<CustomTimerPreset?> {
        return dao.getById(id)
    }

    /**
     * 새로운 커스텀 프리셋 추가
     *
     * @param preset 추가할 프리셋
     */
    suspend fun insert(preset: CustomTimerPreset) {
        dao.insert(preset)
    }

    /**
     * 커스텀 프리셋 업데이트
     *
     * @param preset 업데이트할 프리셋
     */
    suspend fun update(preset: CustomTimerPreset) {
        dao.update(preset)
    }

    /**
     * 커스텀 프리셋 삭제
     *
     * @param id 삭제할 프리셋 ID
     */
    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    /**
     * 표시 순서 업데이트
     *
     * @param id 프리셋 ID
     * @param displayOrder 새 표시 순서
     */
    suspend fun updateDisplayOrder(id: String, displayOrder: Int) {
        dao.updateDisplayOrder(id, displayOrder)
    }

    /**
     * 사용 횟수 증가
     *
     * @param id 프리셋 ID
     */
    suspend fun incrementUsageCount(id: String) {
        dao.incrementUsageCount(id)
    }

    /**
     * 총 개수 조회
     *
     * @return 총 개수
     */
    suspend fun getCount(): Int {
        return dao.getCount()
    }

    /**
     * 사용 횟수가 가장 많은 프리셋 조회
     *
     * @param limit 조회할 개수 (기본 3개)
     * @return 프리셋 리스트 (Flow)
     */
    fun getTopUsed(limit: Int = 3): Flow<List<CustomTimerPreset>> {
        return dao.getTopUsed(limit)
    }
}

