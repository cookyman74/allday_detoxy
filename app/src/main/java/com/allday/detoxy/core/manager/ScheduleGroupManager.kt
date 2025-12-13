package com.allday.detoxy.core.manager

import android.content.Context
import android.location.Location
import android.util.Log
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.domain.repository.ScheduleGroupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
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
    private val conflictResolver: LocationConflictResolver,  // 🆕 Phase 3: 위치 충돌 해소
    private val geofenceManager: AutoRunGeofenceManager, // 🆕 3차 고도화: Geofence 관리 통합
    private val fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient // 🆕 v8.1: 명시적 위치 확인
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
     * 3. 그룹 내 연결된 위치의 Geofence 등록 (옵션)
     * 4. 🆕 v8: 수동 제어(INACTIVE/PAUSED) 상태 해제 (자동 모드 복귀)
     *
     * ## 🆕 다중 활성화 지원
     * - 여러 그룹이 동시에 활성화 가능 (독립적 on/off 스위치)
     * - 실제 실행은 위치/시간에 따라 자동 결정 (LocationConflictResolver, NonLocationScheduleManager)
     * - 사용자가 필요에 따라 원하는 그룹을 자유롭게 활성화/비활성화
     *
     * ## Geofence 재등록 방지
     * - Geofence 진입으로 인해 활성화될 때는 Geofence를 다시 등록하지 않음 (무한 루프 방지)
     *
     * @param groupId 활성화할 시간표 그룹 ID
     * @param updateGeofences Geofence 등록 여부 (기본값 true, Geofence 트리거 시 false)
     * @return Result<Unit> 성공 시 Success, 실패 시 Failure
     */
    suspend fun activateGroup(groupId: String, updateGeofences: Boolean = true): Result<Unit> = runCatching {
        Log.i(TAG, "🔄 Activating schedule group: $groupId (updateGeofences=$updateGeofences)")

        // 🆕 v8: 수동 제어 상태 해제 (자동 모드로 복귀)
        // 활성화한다는 것은 곧 '사용하겠다'는 의미이므로, 기존의 비활성화/일시중지 상태를 초기화함
        repository.updateManualOverride(groupId, null, null)

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
            // 🆕 현재 시간이 시간표 범위 내에 있으면 즉시 실행
            if (isCurrentTimeWithinTimeSlot(autoRun)) {
                Log.i(TAG, "⏰ Current time is within time slot, starting timer immediately: ${autoRun.label ?: autoRun.id}")
                val immediateStarted = alarmManager.startTimerImmediatelyIfInRange(autoRun)
                if (immediateStarted) {
                    Log.i(TAG, "✅ Timer started immediately: ${autoRun.label ?: autoRun.id}")
                    successCount++
                    // 다음 알람도 정상적으로 등록 (내일 같은 시간)
                    alarmManager.scheduleTimeBasedAutoRun(autoRun)
                } else {
                    Log.w(TAG, "⚠️ Failed to start timer immediately, falling back to normal scheduling: ${autoRun.label ?: autoRun.id}")
                    // 즉시 실행 실패 시 정상 알람 등록
                    val success = alarmManager.scheduleTimeBasedAutoRun(autoRun)
                    if (success) {
                        successCount++
                    } else {
                        failCount++
                    }
                }
            } else {
                // 현재 시간이 범위 밖이면 정상 알람 등록
                val success = alarmManager.scheduleTimeBasedAutoRun(autoRun)
                if (success) {
                    successCount++
                    Log.d(TAG, "✅ Alarm scheduled: ${autoRun.label ?: autoRun.id}")
                } else {
                    failCount++
                    Log.w(TAG, "⚠️ Failed to schedule alarm: ${autoRun.label ?: autoRun.id}")
                }
            }
        }

        // 3. 그룹 내 연결된 위치의 Geofence 등록 (옵션)
        if (updateGeofences) {
            val linkedLocations = repository.getLinkedLocations(groupId)
            val enabledLocations = linkedLocations.filter { it.isEnabled }
            
            Log.d(TAG, "📍 Registering ${enabledLocations.size} geofences for group: $groupId")
            
            enabledLocations.forEach { location ->
                // Geofence 등록 시 INITIAL_TRIGGER_ENTER가 설정되어 있으므로,
                // 이미 해당 위치에 있다면 잠시 후 자동으로 Geofence 진입 이벤트가 발생함.
                // 따라서 별도의 위치 확인 로직 없이도 "켜자마자 위치 확인" 동작이 수행됨.
                val result = geofenceManager.addGeofence(location)
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Geofence added: ${location.label} (Expecting INITIAL_TRIGGER if inside)")
                } else {
                    Log.w(TAG, "⚠️ Failed to add geofence: ${location.label} (${result.exceptionOrNull()?.message})")
                }
            }
        }

        Log.i(TAG, "✅ Schedule group activated: $groupId (Success: $successCount, Failed: $failCount)")
    }

    /**
     * 현재 시간이 시간표 범위 내에 있는지 확인
     * 
     * 시간표 시작 시간( hour:minute)부터 종료 시간(시작 시간 + durationMinutes)까지의 범위 내에 현재 시간이 있는지 확인합니다.
     * 
     * @param autoRun 시간 기반 자동 실행 설정
     * @return true: 현재 시간이 범위 내, false: 범위 밖
     */
    private fun isCurrentTimeWithinTimeSlot(autoRun: TimeBasedAutoRun): Boolean {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_WEEK)
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentTimeMinutes = currentHour * 60 + currentMinute
        
        // 요일 확인
        val enabledDays = parseEnabledDays(autoRun.enabledDays)
        val dayCode = when (currentDay) {
            Calendar.SUNDAY -> "SUN"
            Calendar.MONDAY -> "MON"
            Calendar.TUESDAY -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY -> "THU"
            Calendar.FRIDAY -> "FRI"
            Calendar.SATURDAY -> "SAT"
            else -> return false
        }
        
        if (!enabledDays.contains(dayCode)) {
            return false
        }
        
        // 시간표 범위 계산
        val startTimeMinutes = autoRun.hour * 60 + autoRun.minute
        val endTimeMinutes = startTimeMinutes + autoRun.durationMinutes
        
        // 현재 시간이 범위 내에 있는지 확인
        val isWithinRange = currentTimeMinutes >= startTimeMinutes && currentTimeMinutes < endTimeMinutes
        
        Log.d(TAG, "⏰ Time slot check: current=$currentTimeMinutes (${currentHour}:${currentMinute}), range=$startTimeMinutes~$endTimeMinutes (${autoRun.hour}:${autoRun.minute} + ${autoRun.durationMinutes}분), within=$isWithinRange")
        
        return isWithinRange
    }
    
    /**
     * 요일 문자열 파싱 (JSON 배열)
     */
    private fun parseEnabledDays(enabledDaysJson: String): Set<String> {
        return try {
            val json = org.json.JSONArray(enabledDaysJson)
            (0 until json.length()).map { json.getString(it) }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    /**
     * 시간표 그룹 비활성화
     *
     * ## 처리 흐름
     * 1. 해당 그룹 비활성화
     * 2. 그룹 내 모든 시간대의 알람 취소
     * 3. Geofence는 유지 (위치 진입 감지를 위해)
     * 4. 🆕 v8: 사용자 액션인 경우 수동 제어(INACTIVE) 상태 설정
     *
     * @param groupId 비활성화할 시간표 그룹 ID
     * @param isUserAction 사용자 명시적 액션 여부 (true: INACTIVE 설정, false: 상태 유지)
     * @return Result<Unit> 성공 시 Success, 실패 시 Failure
     */
    suspend fun deactivateGroup(groupId: String, isUserAction: Boolean = false): Result<Unit> = runCatching {
        Log.i(TAG, "🔄 Deactivating schedule group: $groupId (isUserAction=$isUserAction)")

        // 🆕 v8: 사용자 액션인 경우 manualOverrideState를 INACTIVE로 설정
        // 이렇게 해야 Geofence 진입 시 Receiver가 'INACTIVE'를 확인하고 자동 실행을 차단함
        if (isUserAction) {
            repository.updateManualOverride(groupId, "INACTIVE", null)
            Log.d(TAG, "🚫 Manual override set to INACTIVE: $groupId")
        }

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

        // 🔧 Critical Fix: Geofence는 해제하지 않음!
        // Geofence가 항상 등록되어 있어야 위치 진입 시 자동으로 스케줄 그룹을 활성화할 수 있음
        // 스케줄 그룹 비활성화 시 알람만 취소하고 Geofence는 유지
        val linkedLocations = repository.getLinkedLocations(groupId)
        Log.d(TAG, "📍 Keeping ${linkedLocations.size} geofences for group: $groupId (for auto-activation on location enter)")

        Log.i(TAG, "✅ Schedule group deactivated: $groupId (${timeBasedAutoRuns.size} alarms cancelled, geofences kept for auto-activation)")
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

        // 3. 시간표 그룹 활성화 (Geofence 재등록 방지)
        activateGroup(scheduleGroupId, updateGeofences = false).getOrThrow()

        // 4. 히스테리시스 타이머 기록 (위치 전환 추적)
        conflictResolver.recordActivation(winner.id)

        Log.i(TAG, "✅ Schedule group activated by location: $scheduleGroupId (location: ${winner.label})")
    }
    /**
     * 시간표 그룹 자동 모드 시작 (수동 ON)
     *
     * 사용자가 UI에서 스위치를 켰을 때 호출됩니다.
     * 무조건 활성화하지 않고, Geofence를 등록하여 위치에 따라 활성화/비활성화 여부를 결정합니다.
     *
     * ## 동작 방식
     * 1. manualOverrideState = null (자동 모드)
     * 2. isActive = false (일단끔, 위치 확인 대기)
     * 3. Geofence 등록 (INITIAL_TRIGGER_ENTER 사용)
     *    - 위치 내부라면: Receiver가 activateGroup 호출 -> 활성화
     *    - 위치 외부라면: 아무 일도 안 일어남 -> 비활성 유지
     *
     * @param groupId 그룹 ID
     */
    /**
     * 시간표 그룹 자동 모드 시작 (수동 ON)
     *
     * 사용자가 UI에서 스위치를 켰을 때 호출됩니다.
     * 무조건 활성화하지 않고, Geofence를 등록하여 위치에 따라 활성화/비활성화 여부를 결정합니다.
     * 
     * ## 동작 방식 (v8.1 개선)
     * 1. manualOverrideState = null (자동 모드)
     * 2. isActive = false (일단끔, 위치 확인 대기)
     * 3. Geofence 등록 (미래의 이벤트를 위해)
     * 4. [NEW] 명시적 위치 확인 (현재 상태 동기화)
     *    - INITIAL_TRIGGER에만 의존하지 않고, getLastLocation을 통해 즉시 확인
     *    - 위치 내부라면: activateGroup 호출 -> 활성화
     *    - 위치 외부라면: 비활성 유지
     *
     * @param groupId 그룹 ID
     */
    suspend fun startAutoMode(groupId: String): Result<Unit> = runCatching {
        Log.i(TAG, "🔄 Starting Auto Mode for group: $groupId")
        
        // 1. 수동 제어 해제
        repository.updateManualOverride(groupId, null, null)
        
        // 2. 일단 비활성화 (모니터링 모드)
        repository.toggleActive(groupId, false)
        Log.d(TAG, "📉 Set to inactive (waiting for location check)")
        
        // 3. Geofence 등록 (미래 감지용)
        val linkedLocations = repository.getLinkedLocations(groupId)
        val enabledLocations = linkedLocations.filter { it.isEnabled }
        
        Log.d(TAG, "📍 Registering ${enabledLocations.size} geofences for Auto Mode")
        
        enabledLocations.forEach { location ->
            geofenceManager.addGeofence(location)
        }
        
        // 4. [Critical] 명시적 위치 확인
        // Geofence INITIAL_TRIGGER가 불안정할 수 있으므로, 현재 위치를 직접 가져와서 확인
        try {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        Log.i(TAG, "📍 Current location found: ${location.latitude}, ${location.longitude} (Accuracy: ${location.accuracy}m)")
                        
                        // 현재 위치가 등록된 Geofence 반경 내에 있는지 확인
                        val match = enabledLocations.find { target ->
                            val results = FloatArray(1)
                            Location.distanceBetween(
                                location.latitude, location.longitude,
                                target.latitude, target.longitude,
                                results
                            )
                            val distanceInMeters = results[0]
                            distanceInMeters <= target.radiusMeters
                        }
                        
                        if (match != null) {
                            Log.i(TAG, "✅ User is inside location: ${match.label} (Distance match). Activating immediately.")
                            // Coroutine scope가 필요하므로 GlobalScope 또는 viewModelScope를 써야 하지만,
                            // 여기서는 runBlocking이나 suspend 함수 내 호출을 보장해야 함.
                            // fusedLocationClient callback은 메인 쓰레드 등에서 비동기 실행됨.
                            // 안전하게 동기화하기 위해 CoroutineScope를 사용해 activate 호출
                            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                activateGroup(groupId, updateGeofences = false)
                            }
                        } else {
                            Log.i(TAG, "ℹ️ User is outside all locations. Staying INACTIVE.")
                        }
                    } else {
                        Log.w(TAG, "⚠️ Last location is null. Relying on Geofence trigger.")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Explicit location check failed: ${e.message}")
        }
    }
}

