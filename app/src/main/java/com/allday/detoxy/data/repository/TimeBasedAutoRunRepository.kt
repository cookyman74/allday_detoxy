package com.allday.detoxy.data.repository

import com.allday.detoxy.data.local.dao.TimeBasedAutoRunDao
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 시간 기반 자동 실행 Repository
 *
 * TimeBasedAutoRun 데이터에 대한 비즈니스 로직 및 데이터 접근 추상화를 제공합니다.
 *
 * @param dao TimeBasedAutoRunDao 인스턴스
 */
@Singleton
class TimeBasedAutoRunRepository @Inject constructor(
    private val dao: TimeBasedAutoRunDao
) {
    /**
     * 모든 시간 기반 자동 실행 목록 조회 (Flow)
     *
     * @return TimeBasedAutoRun 리스트 Flow
     */
    fun getAll(): Flow<List<TimeBasedAutoRun>> {
        return dao.getAll()
    }

    /**
     * 특정 ID의 시간 기반 자동 실행 조회 (Flow)
     *
     * @param id 조회할 자동 실행 ID
     * @return TimeBasedAutoRun Flow (nullable)
     */
    fun getById(id: String): Flow<TimeBasedAutoRun?> {
        return dao.getById(id)
    }

    /**
     * 활성화된 시간 기반 자동 실행 목록 조회 (Flow)
     *
     * @return 활성화된 TimeBasedAutoRun 리스트 Flow
     */
    fun getEnabled(): Flow<List<TimeBasedAutoRun>> {
        return dao.getEnabled()
    }

    /**
     * 새로운 시간 기반 자동 실행 추가
     *
     * @param autoRun 추가할 TimeBasedAutoRun
     */
    suspend fun insert(autoRun: TimeBasedAutoRun) {
        dao.insert(autoRun)
    }

    /**
     * 시간 기반 자동 실행 업데이트
     *
     * @param autoRun 업데이트할 TimeBasedAutoRun
     */
    suspend fun update(autoRun: TimeBasedAutoRun) {
        dao.update(autoRun)
    }

    /**
     * 시간 기반 자동 실행 삭제
     *
     * @param id 삭제할 자동 실행 ID
     */
    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    /**
     * 시간 기반 자동 실행 활성화/비활성화 토글
     *
     * @param id 토글할 자동 실행 ID
     * @param isEnabled 활성화 여부
     */
    suspend fun setEnabled(id: String, isEnabled: Boolean) {
        dao.setEnabled(id, isEnabled)
    }

    /**
     * 특정 요일에 활성화된 자동 실행 목록 조회
     *
     * @param dayOfWeek 요일 코드 (예: "MON", "TUE")
     * @return 해당 요일에 활성화된 TimeBasedAutoRun 리스트 Flow
     */
    fun getEnabledForDay(dayOfWeek: String): Flow<List<TimeBasedAutoRun>> {
        return dao.getEnabledForDay(dayOfWeek)
    }

    // ==================== v5 추가: ScheduleGroup 지원 ====================

    /**
     * 특정 스케줄 그룹에 속한 시간 기반 자동 실행 조회 (v5+)
     *
     * @param scheduleGroupId 스케줄 그룹 ID
     * @return 해당 그룹에 속한 TimeBasedAutoRun 리스트
     */
    suspend fun getByScheduleGroup(scheduleGroupId: String): List<TimeBasedAutoRun> {
        return dao.getByScheduleGroup(scheduleGroupId)
    }

    /**
     * 특정 스케줄 그룹에서 자동 실행 참조 해제 (v5+)
     *
     * scheduleGroupId를 NULL로 설정하여 독립 모드로 전환합니다.
     *
     * @param scheduleGroupId 스케줄 그룹 ID
     */
    suspend fun unlinkFromGroup(scheduleGroupId: String) {
        dao.unlinkFromGroup(scheduleGroupId)
    }

    /**
     * 특정 스케줄 그룹의 종속 자동 실행 활성화/비활성화 (v5+)
     *
     * isIndependent = 0 (종속 모드)인 자동 실행만 활성화/비활성화합니다.
     * 독립 모드(isIndependent = 1)는 영향 받지 않습니다.
     *
     * @param scheduleGroupId 스케줄 그룹 ID
     * @param isEnabled 활성화 여부
     */
    suspend fun setGroupActive(scheduleGroupId: String, isEnabled: Boolean) {
        dao.setGroupActive(scheduleGroupId, isEnabled)
    }
}
