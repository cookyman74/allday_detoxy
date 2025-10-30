package com.allday.detoxy.data.repository

import com.allday.detoxy.data.local.dao.LocationBasedAutoRunDao
import com.allday.detoxy.data.local.dao.ScheduleGroupDao
import com.allday.detoxy.data.local.dao.TimeBasedAutoRunDao
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.data.local.entity.ScheduleGroup
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.domain.repository.ScheduleGroupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ScheduleGroup Repository 구현체
 *
 * domain 레이어 인터페이스의 구현체로, data 레이어에 위치합니다.
 * 참조 무결성 관리를 포함한 CRUD 작업을 담당합니다.
 *
 * ## 참조 무결성 보장
 * - ScheduleGroup 삭제 시: 연결된 자동 실행의 groupId를 NULL로 설정
 * - ScheduleGroup 비활성화 시: 종속 자동 실행(isIndependent=false)만 비활성화
 *
 * @param scheduleGroupDao ScheduleGroup DAO
 * @param timeBasedAutoRunDao TimeBasedAutoRun DAO (참조 무결성 관리)
 * @param locationBasedAutoRunDao LocationBasedAutoRun DAO (참조 무결성 관리)
 *
 * @see com.allday.detoxy.domain.repository.ScheduleGroupRepository
 */
@Singleton
class ScheduleGroupRepositoryImpl @Inject constructor(
    private val scheduleGroupDao: ScheduleGroupDao,
    private val timeBasedAutoRunDao: TimeBasedAutoRunDao,
    private val locationBasedAutoRunDao: LocationBasedAutoRunDao
) : ScheduleGroupRepository {

    // ==================== Flow (실시간 관찰) ====================

    override fun getAll(): Flow<List<ScheduleGroup>> {
        return scheduleGroupDao.getAll()
    }

    override fun getActive(): Flow<List<ScheduleGroup>> {
        return scheduleGroupDao.getActive()
    }

    override fun getById(id: String): Flow<ScheduleGroup?> {
        return scheduleGroupDao.getById(id)
    }

    // ==================== CRUD 메서드 ====================

    override suspend fun insert(scheduleGroup: ScheduleGroup) {
        scheduleGroupDao.insert(scheduleGroup)
    }

    override suspend fun update(scheduleGroup: ScheduleGroup) {
        scheduleGroupDao.update(scheduleGroup)
    }

    override suspend fun delete(scheduleGroupId: String) {
        // 1. 참조 무결성: 연결된 자동 실행의 groupId를 NULL로 설정
        unlinkAllAutoRuns(scheduleGroupId)
        
        // 2. 스케줄 그룹 삭제
        scheduleGroupDao.deleteById(scheduleGroupId)
    }

    override suspend fun toggleActive(scheduleGroupId: String, isActive: Boolean) {
        // 1. 스케줄 그룹 활성화/비활성화
        scheduleGroupDao.setActive(scheduleGroupId, isActive)
        
        // 2. 종속 자동 실행 활성화/비활성화 (isIndependent=false만)
        timeBasedAutoRunDao.setGroupActive(scheduleGroupId, isActive)
    }

    // ==================== 참조 무결성 관리 ====================

    override suspend fun getLinkedTimeBasedAutoRuns(scheduleGroupId: String): List<TimeBasedAutoRun> {
        return timeBasedAutoRunDao.getByScheduleGroup(scheduleGroupId)
    }

    override suspend fun getLinkedLocations(scheduleGroupId: String): List<LocationBasedAutoRun> {
        return locationBasedAutoRunDao.getByLinkedGroup(scheduleGroupId)
    }

    override suspend fun unlinkAllAutoRuns(scheduleGroupId: String) {
        // TimeBasedAutoRun의 scheduleGroupId를 NULL로 설정
        timeBasedAutoRunDao.unlinkFromGroup(scheduleGroupId)
        
        // LocationBasedAutoRun의 linkedScheduleGroupId를 NULL로 설정
        locationBasedAutoRunDao.unlinkFromGroup(scheduleGroupId)
    }
}

