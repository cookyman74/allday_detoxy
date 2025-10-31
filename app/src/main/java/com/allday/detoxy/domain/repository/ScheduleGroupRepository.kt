package com.allday.detoxy.domain.repository

import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.data.local.entity.ScheduleGroup
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import kotlinx.coroutines.flow.Flow

/**
 * ScheduleGroup Repository 인터페이스
 *
 * Clean Architecture의 domain 계층 인터페이스
 * 구현체는 data 계층에 위치 (ScheduleGroupRepositoryImpl)
 *
 * ## 주요 기능
 * 1. ScheduleGroup CRUD
 * 2. 참조 무결성 관리 (Soft Reference)
 * 3. 그룹 활성화/비활성화
 *
 * ## 참조 무결성 정책
 * - ScheduleGroup 삭제 시: 연결된 자동 실행의 groupId를 NULL로 설정 (독립 모드 전환)
 * - ScheduleGroup 비활성화 시: 종속 자동 실행(isIndependent=false)만 비활성화
 * - TimeBasedAutoRun/LocationBasedAutoRun 삭제 시: ScheduleGroup은 영향 없음
 *
 * @see com.allday.detoxy.data.repository.ScheduleGroupRepositoryImpl
 * @see com.allday.detoxy.data.local.entity.ScheduleGroup
 */
interface ScheduleGroupRepository {

    // ==================== Flow (실시간 관찰) ====================

    /**
     * 모든 스케줄 그룹 조회 (생성 시간 역순)
     *
     * @return 스케줄 그룹 리스트 Flow
     */
    fun getAll(): Flow<List<ScheduleGroup>>

    /**
     * 활성화된 스케줄 그룹만 조회
     *
     * @return 활성화된 스케줄 그룹 리스트 Flow
     */
    fun getActive(): Flow<List<ScheduleGroup>>

    /**
     * ID로 특정 스케줄 그룹 조회
     *
     * @param id 스케줄 그룹 ID
     * @return 스케줄 그룹 Flow (nullable)
     */
    fun getById(id: String): Flow<ScheduleGroup?>

    // ==================== CRUD 메서드 ====================

    /**
     * 새로운 스케줄 그룹 추가
     *
     * @param scheduleGroup 추가할 스케줄 그룹
     */
    suspend fun insert(scheduleGroup: ScheduleGroup)

    /**
     * 스케줄 그룹 업데이트
     *
     * @param scheduleGroup 업데이트할 스케줄 그룹
     */
    suspend fun update(scheduleGroup: ScheduleGroup)

    /**
     * 스케줄 그룹 삭제 (참조 무결성 보장)
     *
     * ## 삭제 프로세스
     * 1. 연결된 TimeBasedAutoRun의 scheduleGroupId를 NULL로 설정
     * 2. 연결된 LocationBasedAutoRun의 linkedScheduleGroupId를 NULL로 설정
     * 3. 스케줄 그룹 삭제
     *
     * @param scheduleGroupId 삭제할 스케줄 그룹 ID
     */
    suspend fun delete(scheduleGroupId: String)

    /**
     * 스케줄 그룹 활성화/비활성화
     *
     * ## 비활성화 시 동작
     * - 연결된 TimeBasedAutoRun 중 종속 모드(isIndependent=false)만 비활성화
     * - 독립 모드(isIndependent=true)는 영향 없음
     *
     * @param scheduleGroupId 스케줄 그룹 ID
     * @param isActive 활성화 여부
     */
    suspend fun toggleActive(scheduleGroupId: String, isActive: Boolean)

    /**
     * 스케줄 그룹 활성화/비활성화 및 마지막 활성화 시각 업데이트 (v6+)
     *
     * ## 동작
     * - 스케줄 그룹 활성화/비활성화
     * - 활성화 시: lastActivatedAt을 현재 시각으로 업데이트
     * - 비활성화 시: lastActivatedAt은 유지 (null로 변경하지 않음)
     * - 연결된 TimeBasedAutoRun 중 종속 모드(isIndependent=false)만 비활성화
     *
     * @param scheduleGroupId 스케줄 그룹 ID
     * @param isActive 활성화 여부
     * @param timestamp 마지막 활성화 시각 (활성화 시 현재 시각, 비활성화 시 null)
     */
    suspend fun toggleActiveWithTimestamp(scheduleGroupId: String, isActive: Boolean, timestamp: Long? = null)

    // ==================== 참조 무결성 관리 ====================

    /**
     * 특정 그룹에 연결된 시간 기반 자동 실행 조회
     *
     * @param scheduleGroupId 스케줄 그룹 ID
     * @return 연결된 TimeBasedAutoRun 리스트
     */
    suspend fun getLinkedTimeBasedAutoRuns(scheduleGroupId: String): List<TimeBasedAutoRun>

    /**
     * 특정 그룹에 연결된 위치 기반 자동 실행 조회
     *
     * @param scheduleGroupId 스케줄 그룹 ID
     * @return 연결된 LocationBasedAutoRun 리스트
     */
    suspend fun getLinkedLocations(scheduleGroupId: String): List<LocationBasedAutoRun>

    /**
     * 특정 그룹에 연결된 시간 기반 자동 실행 개수 조회
     *
     * @param scheduleGroupId 스케줄 그룹 ID
     * @return 연결된 TimeBasedAutoRun 개수
     */
    suspend fun getLinkedTimeBasedAutoRunCount(scheduleGroupId: String): Int

    /**
     * 특정 그룹에 연결된 위치 기반 자동 실행 개수 조회
     *
     * @param scheduleGroupId 스케줄 그룹 ID
     * @return 연결된 LocationBasedAutoRun 개수
     */
    suspend fun getLinkedLocationCount(scheduleGroupId: String): Int

    /**
     * 특정 그룹에 연결된 모든 자동 실행의 참조 해제
     *
     * ## 참조 해제 동작
     * - TimeBasedAutoRun: scheduleGroupId → NULL
     * - LocationBasedAutoRun: linkedScheduleGroupId → NULL
     *
     * @param scheduleGroupId 스케줄 그룹 ID
     */
    suspend fun unlinkAllAutoRuns(scheduleGroupId: String)
}

