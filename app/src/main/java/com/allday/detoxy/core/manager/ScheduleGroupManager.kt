package com.allday.detoxy.core.manager

import android.content.Context
import android.location.Location
import android.util.Log
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.domain.repository.ScheduleGroupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ScheduleGroup 관리 로직
 *
 * 시간표 그룹의 활성화/비활성화 및 연결된 시간대 알람 관리를 담당합니다.
 *
 * ## 주요 기능
 * - 시간표 그룹 활성화: 그룹 내 모든 시간대의 알람 등록
 * - 시간표 그룹 비활성화: 그룹 내 모든 시간대의 알람 취소
 * - 🆕 다중 활성화 지원: 여러 그룹이 동시에 활성화 가능 (위치/시간에 따라 자동 실행)
 *
 * ## 3차 고도화 핵심 로직
 * - 위치 진입 시 시간표 자동 실행 선택 → 시간대 알람 자동 등록
 * - 위치 이탈 시 시간표 비활성화 → 시간대 알람 자동 취소
 *
 * ## 3.5차 고도화 핵심 로직 (Phase 3)
 * - 위치 충돌 해소: LocationConflictResolver를 통한 우선순위 규칙 적용
 * - 히스테리시스 타이머: 120초간 기존 위치 유지
 *
 * ## 활성화 vs 실행
 * - **활성화(isActive)**: 이 스케줄을 사용할지 말지의 on/off (여러 그룹 동시 활성화 가능)
 * - **실행**: 활성화된 그룹 중 현재 위치/시간에 맞는 것을 자동 선택
 *
 * @param context Application Context
 * @param repository ScheduleGroupRepository
 * @param alarmManager AutoRunAlarmManager (시간대 알람 관리)
 * @param conflictResolver LocationConflictResolver (위치 충돌 해소)
 *
 * @see com.allday.detoxy.domain.repository.ScheduleGroupRepository
 * @see com.allday.detoxy.core.manager.AutoRunAlarmManager
 * @see com.allday.detoxy.core.manager.LocationConflictResolver
 * @see docs/03_complex_time&location_todolist.md §2.1
 * @see docs/03.5_스케쥴가동프로세스.md
 */
@Singleton
class ScheduleGroupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ScheduleGroupRepository,
    private val alarmManager: AutoRunAlarmManager,
    private val conflictResolver: LocationConflictResolver  // 🆕 Phase 3: 위치 충돌 해소
) {
    companion object {
        private const val TAG = "ScheduleGroupManager"
    }

    /**
     * 시간표 그룹 활성화
     *
     * ## 처리 흐름
     * 1. 해당 그룹 활성화 (lastActivatedAt 업데이트)
     * 2. 그룹 내 활성화된 시간대의 알람 등록
     *
     * ## 🆕 다중 활성화 지원
     * - 여러 그룹이 동시에 활성화 가능 (독립적 on/off 스위치)
     * - 실제 실행은 위치/시간에 따라 자동 결정 (LocationConflictResolver, NonLocationScheduleManager)
     * - 사용자가 필요에 따라 원하는 그룹을 자유롭게 활성화/비활성화
     *
     * @param groupId 활성화할 시간표 그룹 ID
     * @return Result<Unit> 성공 시 Success, 실패 시 Failure
     */
    suspend fun activateGroup(groupId: String): Result<Unit> = runCatching {
        Log.i(TAG, "🔄 Activating schedule group: $groupId")

        // 1. 해당 그룹 활성화 (lastActivatedAt 업데이트)
        val timestamp = System.currentTimeMillis()
        repository.toggleActiveWithTimestamp(groupId, true, timestamp)
        Log.d(TAG, "✅ ScheduleGroup activated: $groupId (timestamp: $timestamp)")

        // 2. 그룹 내 활성화된 시간대 알람 등록
        val timeBasedAutoRuns = repository.getLinkedTimeBasedAutoRuns(groupId)
        val enabledAutoRuns = timeBasedAutoRuns.filter { it.isEnabled }

        Log.d(TAG, "📅 Scheduling ${enabledAutoRuns.size} time-based auto-runs (enabled only)")

        var successCount = 0
        var failCount = 0

        enabledAutoRuns.forEach { autoRun ->
            val success = alarmManager.scheduleTimeBasedAutoRun(autoRun)
            if (success) {
                successCount++
                Log.d(TAG, "✅ Alarm scheduled: ${autoRun.label ?: autoRun.id}")
            } else {
                failCount++
                Log.w(TAG, "⚠️ Failed to schedule alarm: ${autoRun.label ?: autoRun.id}")
            }
        }

        Log.i(TAG, "✅ Schedule group activated: $groupId (Success: $successCount, Failed: $failCount)")
    }

    /**
     * 시간표 그룹 비활성화
     *
     * ## 처리 흐름
     * 1. 해당 그룹 비활성화
     * 2. 그룹 내 모든 시간대의 알람 취소
     *
     * @param groupId 비활성화할 시간표 그룹 ID
     * @return Result<Unit> 성공 시 Success, 실패 시 Failure
     */
    suspend fun deactivateGroup(groupId: String): Result<Unit> = runCatching {
        Log.i(TAG, "🔄 Deactivating schedule group: $groupId")

        // 1. 그룹 비활성화 (lastActivatedAt은 유지)
        repository.toggleActive(groupId, false)
        Log.d(TAG, "🔻 ScheduleGroup deactivated: $groupId")

        // 2. 그룹 내 모든 시간대 알람 취소
        val timeBasedAutoRuns = repository.getLinkedTimeBasedAutoRuns(groupId)

        Log.d(TAG, "📅 Cancelling ${timeBasedAutoRuns.size} time-based auto-runs")

        timeBasedAutoRuns.forEach { autoRun ->
            alarmManager.cancelTimeBasedAutoRun(autoRun.id)
            Log.d(TAG, "🗑️ Alarm cancelled: ${autoRun.label ?: autoRun.id}")
        }

        Log.i(TAG, "✅ Schedule group deactivated: $groupId (${timeBasedAutoRuns.size} alarms cancelled)")
    }

    /**
     * 현재 활성화된 시간표 그룹 조회
     *
     * @return 활성화된 ScheduleGroup (없으면 null)
     */
    suspend fun getActiveGroup(): com.allday.detoxy.data.local.entity.ScheduleGroup? {
        return repository.getActive().first().firstOrNull()
    }

    /**
     * 위치 기반 시간표 활성화 (충돌 해소 포함) (Phase 3)
     *
     * 여러 위치 반경이 겹칠 때 LocationConflictResolver를 통해 우선순위 규칙을 적용하여
     * 단일 위치를 선택하고, 해당 위치에 연결된 시간표 그룹을 활성화합니다.
     *
     * ## 처리 흐름
     * 1. LocationConflictResolver를 통해 충돌 해소 (4단계 우선순위 규칙 적용)
     * 2. 선택된 위치에 연결된 시간표 그룹 확인
     * 3. 시간표 그룹 활성화 (activateGroup 호출)
     * 4. 히스테리시스 타이머 기록 (conflictResolver.recordActivation)
     *
     * ## 우선순위 규칙 (LocationConflictResolver)
     * 1. 면적(반경) 우선: 반경이 작을수록 구체적
     * 2. 히스테리시스 (120초): 직전 활성 위치 유지
     * 3. 거리 우선: 사용자에게 가장 가까운 위치
     * 4. 최근 수정(updatedAt) 우선: 최신 의도 반영
     *
     * ## 에러 처리
     * - 후보 위치 없음: IllegalStateException
     * - 연결된 시간표 없음: IllegalStateException
     * - 시간표 활성화 실패: 예외 전파
     *
     * @param currentLocation 사용자의 현재 위치
     * @param candidates 반경 내 포함되는 위치 후보들 (activateScheduleOnEnter == true)
     * @return Result<Unit> 성공 시 Success, 실패 시 Failure
     *
     * @see LocationConflictResolver.resolveConflict
     * @see activateGroup
     */
    suspend fun activateGroupByLocation(
        currentLocation: Location,
        candidates: List<LocationBasedAutoRun>
    ): Result<Unit> = runCatching {
        Log.i(TAG, "🌍 Activating group by location")
        Log.d(TAG, "  Candidates: ${candidates.size}")
        candidates.forEach { 
            Log.d(TAG, "    - ${it.label} (radius: ${it.radiusMeters}m, scheduleGroupId: ${it.linkedScheduleGroupId})")
        }

        // 1. 충돌 해소 (4단계 우선순위 규칙 적용)
        val winner = conflictResolver.resolveConflict(currentLocation, candidates)
            ?: throw IllegalStateException("No suitable location found after conflict resolution")

        Log.i(TAG, "🏆 Winner location: ${winner.label} (ID: ${winner.id})")

        // 2. 연결된 시간표 그룹 확인
        val scheduleGroupId = winner.linkedScheduleGroupId
            ?: throw IllegalStateException("Location has no linked schedule group: ${winner.label}")

        Log.d(TAG, "📋 Linked schedule group: $scheduleGroupId")

        // 3. 시간표 그룹 활성화
        activateGroup(scheduleGroupId).getOrThrow()

        // 4. 히스테리시스 타이머 기록 (위치 전환 추적)
        conflictResolver.recordActivation(winner.id)

        Log.i(TAG, "✅ Schedule group activated by location: $scheduleGroupId (location: ${winner.label})")
    }
}

