package com.allday.detoxy.data.repository

import com.allday.detoxy.data.local.dao.LocationBasedAutoRunDao
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 위치 기반 자동 실행 Repository
 *
 * LocationBasedAutoRun 데이터에 대한 비즈니스 로직과 데이터 액세스를 담당합니다.
 *
 * @property dao LocationBasedAutoRunDao
 */
@Singleton
class LocationBasedAutoRunRepository @Inject constructor(
    private val dao: LocationBasedAutoRunDao
) {

    /**
     * 모든 위치 기반 자동 실행 조회
     *
     * @return 자동 실행 리스트 (Flow)
     */
    fun getAll(): Flow<List<LocationBasedAutoRun>> = dao.getAll()

    /**
     * ID로 위치 기반 자동 실행 조회
     *
     * @param id 자동 실행 ID
     * @return 자동 실행 (Flow)
     */
    fun getById(id: String): Flow<LocationBasedAutoRun?> = dao.getById(id)

    /**
     * 활성화된 위치 기반 자동 실행 조회
     *
     * @return 활성화된 자동 실행 리스트 (Flow)
     */
    fun getEnabled(): Flow<List<LocationBasedAutoRun>> = dao.getEnabled()

    /**
     * 새로운 위치 기반 자동 실행 추가
     *
     * @param autoRun 추가할 자동 실행
     */
    suspend fun insert(autoRun: LocationBasedAutoRun) {
        dao.insert(autoRun)
    }

    /**
     * 위치 기반 자동 실행 업데이트
     *
     * @param autoRun 업데이트할 자동 실행
     */
    suspend fun update(autoRun: LocationBasedAutoRun) {
        dao.update(autoRun)
    }

    /**
     * 위치 기반 자동 실행 삭제
     *
     * @param id 삭제할 자동 실행 ID
     */
    suspend fun deleteById(id: String) {
        dao.deleteById(id)
    }

    /**
     * 활성화/비활성화 토글
     *
     * @param id 자동 실행 ID
     * @param isEnabled 활성화 여부
     */
    suspend fun toggleEnabled(id: String, isEnabled: Boolean) {
        dao.toggleEnabled(id, isEnabled)
    }

    /**
     * 총 개수 조회
     *
     * @return 총 개수
     */
    suspend fun getCount(): Int = dao.getCount()

    /**
     * 활성화된 자동 실행 개수 조회
     *
     * @return 활성화된 개수
     */
    suspend fun getEnabledCount(): Int = dao.getEnabledCount()
}

