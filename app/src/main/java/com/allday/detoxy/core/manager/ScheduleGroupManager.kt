package com.allday.detoxy.core.manager

import android.content.Context
import android.util.Log
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
 * - 단일 활성화 원칙: 한 번에 하나의 시간표 그룹만 활성화
 *
 * ## 3차 고도화 핵심 로직
 * - 위치 진입 시 시간표 활성화 → 시간대 알람 자동 등록
 * - 위치 이탈 시 시간표 비활성화 → 시간대 알람 자동 취소
 *
 * @param context Application Context
 * @param repository ScheduleGroupRepository
 * @param alarmManager AutoRunAlarmManager (시간대 알람 관리)
 *
 * @see com.allday.detoxy.domain.repository.ScheduleGroupRepository
 * @see com.allday.detoxy.core.manager.AutoRunAlarmManager
 * @see docs/03_complex_time&location_todolist.md §2.1
 */
@Singleton
class ScheduleGroupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ScheduleGroupRepository,
    private val alarmManager: AutoRunAlarmManager
) {
    companion object {
        private const val TAG = "ScheduleGroupManager"
    }

    /**
     * 시간표 그룹 활성화
     *
     * ## 처리 흐름
     * 1. 다른 모든 그룹 비활성화 (단일 활성화 원칙)
     * 2. 해당 그룹 활성화 (lastActivatedAt 업데이트)
     * 3. 그룹 내 활성화된 시간대의 알람 재등록
     *
     * ## 단일 활성화 원칙
     * - 한 번에 하나의 시간표만 활성화 가능
     * - 새로운 시간표 활성화 시 기존 활성화된 시간표는 자동 비활성화
     * - 이유: 사용자 혼란 방지, 위치별 명확한 시간표 매핑
     *
     * @param groupId 활성화할 시간표 그룹 ID
     * @return Result<Unit> 성공 시 Success, 실패 시 Failure
     */
    suspend fun activateGroup(groupId: String): Result<Unit> = runCatching {
        Log.i(TAG, "🔄 Activating schedule group: $groupId")

        // 1. 다른 모든 그룹 비활성화
        val activeGroups = repository.getActive().first()
        activeGroups.forEach { group ->
            if (group.id != groupId && group.isActive) {
                Log.d(TAG, "🔻 Deactivating other group: ${group.name} (${group.id})")
                deactivateGroup(group.id).getOrThrow()
            }
        }

        // 2. 해당 그룹 활성화 (lastActivatedAt 업데이트)
        val timestamp = System.currentTimeMillis()
        repository.toggleActiveWithTimestamp(groupId, true, timestamp)
        Log.d(TAG, "✅ ScheduleGroup activated: $groupId (timestamp: $timestamp)")

        // 3. 그룹 내 활성화된 시간대 알람 등록
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
}

