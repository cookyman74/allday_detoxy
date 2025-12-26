package com.allday.detoxy.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.allday.detoxy.core.manager.AutoRunGeofenceManager
import com.allday.detoxy.core.manager.NonLocationScheduleManager
import com.allday.detoxy.core.manager.ScheduleGroupManager
import com.allday.detoxy.core.utils.AnalyticsHelper
import com.allday.detoxy.data.local.dao.LocationBasedAutoRunDao
import com.allday.detoxy.data.local.entity.AutoRunLog
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * Geofence 트리거 이벤트를 수신하는 BroadcastReceiver
 *
 * Geofence 진입(ENTER) 시 호출되어 위치 기반 자동 실행을 트리거합니다.
 *
 * 주요 기능:
 * - Geofence ENTER 이벤트 감지
 * - GPS 정확도 추출 및 로깅
 * - 위치 기반 자동 실행 정보 추출
 * - TODO: AutoRunNotificationManager 연동 (Week 3 작업)
 * - TODO: AutoRunLog 기록 (Week 3 작업)
 *
 * @see AutoRunGeofenceManager
 */
class GeofenceTransitionsReceiver : BroadcastReceiver() {
    
    /**
     * Hilt EntryPoint
     *
     * BroadcastReceiver는 @AndroidEntryPoint를 사용할 수 없으므로
     * EntryPointAccessors를 통해 수동으로 의존성을 가져옵니다.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface GeofenceReceiverEntryPoint {
        fun locationBasedAutoRunDao(): LocationBasedAutoRunDao
        fun scheduleGroupManager(): ScheduleGroupManager
        fun nonLocationScheduleManager(): NonLocationScheduleManager  // 🆕 Phase 4: "어디서나 적용" 스케줄
        fun autoRunLogDao(): com.allday.detoxy.data.local.dao.AutoRunLogDao  // 🆕 AutoRunLog 기록용
        fun locationConflictResolver(): com.allday.detoxy.core.manager.LocationConflictResolver  // 🆕 충돌 해소용
        fun scheduleGroupDao(): com.allday.detoxy.data.local.dao.ScheduleGroupDao  // v8: manualOverrideState 확인용
    }
    
    companion object {
        private const val TAG = "GeofenceTransitionsReceiver"

        /**
         * 소스 ID를 SHA-256으로 해시 처리
         *
         * Analytics 개인정보 보호를 위해 소스 ID를 해시 처리합니다.
         *
         * @param sourceId 소스 ID (위치 기반 자동 실행 ID)
         * @return SHA-256 해시값 (16자 hex)
         */
        private fun hashSourceId(sourceId: String): String {
            val bytes = sourceId.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            return digest.joinToString("") { "%02x".format(it) }.take(16)
        }
    }

    // 🔧 v0.10.4: SupervisorJob으로 자식 코루틴 예외가 다른 코루틴에 영향을 주지 않도록 함
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "❌ Uncaught exception in GeofenceTransitionsReceiver scope: ${throwable.message}", throwable)
    })
    
    override fun onReceive(context: Context, intent: Intent) {
        // GeofencingEvent 파싱
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null")
            return
        }
        
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }
        
        // Geofence 트랜지션 타입 확인
        val geofenceTransition = geofencingEvent.geofenceTransition
        
        // ENTER, DWELL, EXIT 이벤트 처리
        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                handleGeofenceTrigger(context, intent, geofencingEvent, "ENTER")
            }
            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                handleGeofenceTrigger(context, intent, geofencingEvent, "DWELL")
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                handleGeofenceExit(context, intent, geofencingEvent)
            }
            else -> {
                Log.w(TAG, "Unexpected geofence transition: $geofenceTransition")
            }
        }
    }
    
    /**
     * Geofence 트리거 처리 (ENTER 또는 DWELL)
     * 
     * ## Phase 3.5 고도화 (위치 충돌 해소 통합)
     * - 모든 트리거된 geofence의 위치를 수집
     * - activateGroupByLocation()을 통해 충돌 해소 후 시간표 활성화
     * - 4단계 우선순위 규칙 자동 적용
     */
    private fun handleGeofenceTrigger(
        context: Context,
        @Suppress("UNUSED_PARAMETER") intent: Intent,
        geofencingEvent: GeofencingEvent,
        @Suppress("UNUSED_PARAMETER") transitionType: String
    ) {
        // 트리거된 Geofence 리스트 가져오기
        val triggeringGeofences = geofencingEvent.triggeringGeofences
        
        if (triggeringGeofences.isNullOrEmpty()) {
            Log.w(TAG, "⚠️ No triggering geofences found")
            return
        }
        
        // 🆕 사용자 현재 위치 (충돌 해소용)
        val userLocation = geofencingEvent.triggeringLocation
        if (userLocation == null) {
            Log.e(TAG, "❌ Triggering location is null, cannot resolve conflicts")
            return
        }
        
        // GPS 정확도 추출 (AutoRunLog에 기록용)
        val gpsAccuracyMeters = userLocation.accuracy
        Log.i(TAG, "📡 GPS Accuracy: ${gpsAccuracyMeters}m")
        Log.i(TAG, "📍 User location: (${userLocation.latitude}, ${userLocation.longitude})")
        
        // EntryPoint를 통해 필요한 의존성 가져오기
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            GeofenceReceiverEntryPoint::class.java
        )
        
                val locationDao = entryPoint.locationBasedAutoRunDao()
                val scheduleManager = entryPoint.scheduleGroupManager()
                val autoRunLogDao = entryPoint.autoRunLogDao()
                val conflictResolver = entryPoint.locationConflictResolver()
                val scheduleGroupDao = entryPoint.scheduleGroupDao()  // v8: manualOverrideState 확인용
                
                // 비동기 작업 (goAsync)
                val pendingResult = goAsync()
                
                // 🔧 v0.10.4: 디버깅 로그 강화
                Log.i(TAG, "📍 [GEOFENCE_ENTER] Processing ${triggeringGeofences.size} geofence(s)")
                val startTime = System.currentTimeMillis()
                
        scope.launch {
            try {
                Log.d(TAG, "⏱️ [GEOFENCE_ENTER] Coroutine started (${System.currentTimeMillis() - startTime}ms after trigger)")
                
                // 🆕 Phase 3: 반경 내 모든 위치 수집 (충돌 해소를 위해)
                val locationIds = triggeringGeofences.map { it.requestId }
                val candidates = mutableListOf<com.allday.detoxy.data.local.entity.LocationBasedAutoRun>()
                
                locationIds.forEach { id ->
                    val location = locationDao.getByIdOnce(id)
                    if (location != null) {
                        // activateScheduleOnEnter가 true이고, 시간표가 연결된 위치만 후보에 추가
                        if (location.isEnabled && location.activateScheduleOnEnter && location.linkedScheduleGroupId != null) {
                            
                            // v8: 스케줄 그룹의 manualOverrideState 확인
                            val scheduleGroupId = location.linkedScheduleGroupId
                            val scheduleGroup = scheduleGroupDao.getByIdOnce(scheduleGroupId)
                            
                            if (scheduleGroup == null) {
                                Log.w(TAG, "⚠️ v8: ScheduleGroup not found: $scheduleGroupId")
                                return@forEach
                            }
                            
                            val overrideState = scheduleGroup.manualOverrideState
                            val pauseUntil = scheduleGroup.pauseUntil
                            val currentTime = System.currentTimeMillis()
                            
                            // v8: 사용자 의도 우선 정책
                            when (overrideState) {
                                "INACTIVE" -> {
                                    // 사용자가 명시적으로 비활성화 → 위치 진입 무시
                                    Log.i(TAG, "⏹️ v8: Location ${location.label} SKIPPED - ScheduleGroup manually INACTIVE")
                                    
                                    // 로그 기록 (SKIPPED)
                                    try {
                                        val log = AutoRunLog(
                                            triggerType = "LOCATION",
                                            triggerSourceId = id,
                                            triggerTime = currentTime,
                                            result = "SKIPPED",
                                            failureReason = "SCHEDULE_GROUP_INACTIVE",
                                            sessionId = null,
                                            gpsAccuracyMeters = gpsAccuracyMeters
                                        )
                                        autoRunLogDao.insert(log)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Failed to record SKIPPED log: ${e.message}")
                                    }
                                    return@forEach  // 이 위치는 건너뛰기
                                }
                                "PAUSED" -> {
                                    // 일시중지 상태 → pauseUntil 확인
                                    if (pauseUntil != null && currentTime < pauseUntil) {
                                        // 아직 만료 안됨 → 위치 진입 무시
                                        val remainingMinutes = (pauseUntil - currentTime) / 60000
                                        Log.i(TAG, "⏸️ v8: Location ${location.label} SKIPPED - ScheduleGroup PAUSED (${remainingMinutes}min remaining)")
                                        
                                        // 로그 기록 (SKIPPED)
                                        try {
                                            val log = AutoRunLog(
                                                triggerType = "LOCATION",
                                                triggerSourceId = id,
                                                triggerTime = currentTime,
                                                result = "SKIPPED",
                                                failureReason = "SCHEDULE_GROUP_PAUSED",
                                                sessionId = null,
                                                gpsAccuracyMeters = gpsAccuracyMeters
                                            )
                                            autoRunLogDao.insert(log)
                                        } catch (e: Exception) {
                                            Log.e(TAG, "❌ Failed to record SKIPPED log: ${e.message}")
                                        }
                                        return@forEach  // 이 위치는 건너뛰기
                                    } else {
                                        // 만료됨 → 자동으로 ACTIVE로 전환하고 정상 처리
                                        Log.i(TAG, "✅ v8: PAUSED expired, clearing override state")
                                        scheduleGroupDao.updateManualOverride(scheduleGroupId, null, null)
                                        // 정상 후보에 추가 (아래로 진행)
                                    }
                                }
                                else -> {
                                    // null 또는 기타 → 자동 모드 (정상 처리)
                                    Log.d(TAG, "✅ v8: Location ${location.label} - ScheduleGroup in AUTO mode")
                                }
                            }
                            
                            candidates.add(location)
                            
                            // Analytics: auto_run_triggered (각 위치마다)
                            AnalyticsHelper.logAutoRunTriggered(
                                triggerType = "LOCATION",
                                sourceIdHash = hashSourceId(id),
                                durationMinutes = 0,  // ScheduleGroup이므로 개별 duration 없음
                                presetType = "SCHEDULE_GROUP"
                            )
                        }
                    }
                }
                
                if (candidates.isEmpty()) {
                    Log.w(TAG, "No eligible locations found (v8: all may have been INACTIVE/PAUSED)")
                    return@launch
                }
                
                // 🔧 Critical Fix: 충돌 해소를 먼저 수행하여 winner 위치 확인
                val triggerTime = System.currentTimeMillis()
                val winner = conflictResolver.resolveConflict(userLocation, candidates)
                
                if (winner == null) {
                    Log.e(TAG, "No winner location found after conflict resolution")
                    
                    // 실패 로그 기록
                    val firstCandidateId = candidates.firstOrNull()?.id
                    if (firstCandidateId != null) {
                        try {
                            val log = AutoRunLog(
                                triggerType = "LOCATION",
                                triggerSourceId = firstCandidateId,
                                triggerTime = triggerTime,
                                result = "FAILED",
                                failureReason = "No winner location found",
                                sessionId = null,
                                gpsAccuracyMeters = gpsAccuracyMeters
                            )
                            autoRunLogDao.insert(log)
                            Log.i(TAG, "✅ AutoRunLog recorded: LOCATION FAILED (no winner)")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Failed to record AutoRunLog: ${e.message}", e)
                        }
                    }
                    return@launch
                }
                
                Log.i(TAG, "🏆 Winner location: ${winner.label} (ID: ${winner.id})")
                
                // 🆕 Phase 3: 충돌 해소 후 시간표 활성화
                val result = scheduleManager.activateGroupByLocation(
                    currentLocation = userLocation,
                    candidates = candidates
                )
                
                if (result.isSuccess) {
                    Log.i(TAG, "Location-based schedule activated: ${winner.label}")
                    
                    // 🆕 v9: 위치 기반 스케줄 scheduleInfoJson 설정 (리뷰 피드백)
                    // 오버레이/세션 종료 다이얼로그에서 목표·할일 표시용
                    winner.scheduleInfoJson?.let { json ->
                        com.allday.detoxy.service.accessibility.FocusAccessibilityService.currentScheduleInfoJson = json
                        Log.i(TAG, "✅ v9: currentScheduleInfoJson set from location-based schedule")
                    }
                    
                    // 🔧 Critical Fix: 위치 기반 자동 실행 AutoRunLog 기록 (winner 위치 사용)
                    try {
                        val log = AutoRunLog(
                            triggerType = "LOCATION",
                            triggerSourceId = winner.id,
                            triggerTime = triggerTime,
                            result = "STARTED",  // 스케줄 활성화 성공
                            failureReason = null,
                            sessionId = null,  // 타이머 시작 후 sessionId로 업데이트됨
                            gpsAccuracyMeters = gpsAccuracyMeters,
                            dwellSeconds = null  // 체류 시간은 타이머 완료 시 계산
                        )
                        autoRunLogDao.insert(log)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to record AutoRunLog: ${e.message}", e)
                    }
                    
                    // TODO: Week 3 - 시간표 활성화 알림 표시
                    // showScheduleActivatedNotification(context, winner.label, scheduleGroupId)
                } else {
                    Log.e(TAG, "Failed to activate schedule", result.exceptionOrNull())
                    
                    // 🔧 Critical Fix: 실패 시 AutoRunLog 기록 (winner 위치 사용)
                    try {
                        val log = AutoRunLog(
                            triggerType = "LOCATION",
                            triggerSourceId = winner.id,
                            triggerTime = triggerTime,
                            result = "FAILED",
                            failureReason = result.exceptionOrNull()?.message ?: "unknown",
                            sessionId = null,
                            gpsAccuracyMeters = gpsAccuracyMeters
                        )
                        autoRunLogDao.insert(log)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to record AutoRunLog: ${e.message}", e)
                    }
                    
                    // Analytics: auto_run_failed
                    AnalyticsHelper.logAutoRunFailed(
                        triggerType = "LOCATION",
                        failureReason = result.exceptionOrNull()?.message ?: "unknown",
                        gpsAccuracyMeters = gpsAccuracyMeters
                    )
                }
                
                // Analytics: GPS 정확도 낮을 때 경고
                if (gpsAccuracyMeters > 100f) {
                    Log.w(TAG, "GPS accuracy is low (${gpsAccuracyMeters}m)")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ [GEOFENCE_ENTER] Error handling geofence enter: ${e.message}", e)
                Log.e(TAG, "❌ [GEOFENCE_ENTER] Stack trace: ${e.stackTraceToString()}")
                
                // Analytics: auto_run_failed
                AnalyticsHelper.logAutoRunFailed(
                    triggerType = "LOCATION",
                    failureReason = e.message ?: "exception",
                    gpsAccuracyMeters = gpsAccuracyMeters
                )
            } catch (t: Throwable) {
                // 🔧 v0.10.4: Throwable까지 catch하여 Error도 로깅
                Log.e(TAG, "❌ [GEOFENCE_ENTER] CRITICAL: Throwable caught: ${t.message}", t)
                Log.e(TAG, "❌ [GEOFENCE_ENTER] Stack trace: ${t.stackTraceToString()}")
            } finally {
                val elapsed = System.currentTimeMillis() - startTime
                Log.i(TAG, "✅ [GEOFENCE_ENTER] Coroutine completed (${elapsed}ms total)")
                pendingResult.finish()
            }
        }
    }
    
    /**
     * Geofence EXIT 처리 (위치 이탈)
     *
     * ## Phase 4: "어디서나 적용" 스케줄 전환
     * 위치 이탈 시 연결된 ScheduleGroup을 비활성화하고,
     * "어디서나 적용" 스케줄을 자동으로 활성화합니다.
     *
     * ## 처리 흐름
     * 1. ScheduleGroup 비활성화 (위치 연결된 경우)
     * 2. "어디서나 적용" 스케줄 평가 및 활성화
     *
     * @param context Context
     * @param intent Intent
     * @param geofencingEvent GeofencingEvent
     */
    private fun handleGeofenceExit(
        context: Context,
        intent: Intent,
        geofencingEvent: GeofencingEvent
    ) {
        // 트리거된 Geofence 리스트 가져오기
        val triggeringGeofences = geofencingEvent.triggeringGeofences
        
        if (triggeringGeofences.isNullOrEmpty()) {
            Log.w(TAG, "⚠️ No triggering geofences found for EXIT")
            return
        }
        
        Log.i(TAG, "🚪 Geofence EXIT detected: ${triggeringGeofences.size} location(s)")
        
        // EntryPoint를 통해 필요한 의존성 가져오기
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            GeofenceReceiverEntryPoint::class.java
        )
        
        val locationDao = entryPoint.locationBasedAutoRunDao()
        val scheduleManager = entryPoint.scheduleGroupManager()
        val nonLocationScheduleManager = entryPoint.nonLocationScheduleManager()  // 🆕 Phase 4
        val scheduleGroupDao = entryPoint.scheduleGroupDao()  // v8: manualOverrideState 확인용
        
        // 비동기 작업 (goAsync)
        val pendingResult = goAsync()
        
        // 🔧 v0.10.4: 디버깅 로그 강화
        Log.i(TAG, "🚪 [GEOFENCE_EXIT] Processing ${triggeringGeofences.size} geofence(s)")
        val startTime = System.currentTimeMillis()
        
        scope.launch {
            try {
                Log.d(TAG, "⏱️ [GEOFENCE_EXIT] Coroutine started (${System.currentTimeMillis() - startTime}ms after trigger)")
                
                var anyScheduleDeactivated = false
                
                // 각 Geofence 처리
                triggeringGeofences.forEach { geofence ->
                    val locationId = geofence.requestId
                    val locationLabel = intent.getStringExtra(AutoRunGeofenceManager.EXTRA_LOCATION_LABEL)
                    
                    Log.d(TAG, """
                        🚪 Processing EXIT:
                        - ID: $locationId
                        - Label: $locationLabel
                    """.trimIndent())
                    
                    // 1. locationId로 LocationBasedAutoRun 조회
                    val location = locationDao.getByIdOnce(locationId)
                    
                    if (location != null && location.deactivateScheduleOnExit && location.linkedScheduleGroupId != null) {
                        val scheduleGroupId = location.linkedScheduleGroupId
                        
                        // v8: 스케줄 그룹의 manualOverrideState 확인
                        val scheduleGroup = scheduleGroupDao.getByIdOnce(scheduleGroupId)
                        
                        if (scheduleGroup == null) {
                            Log.w(TAG, "⚠️ v8: ScheduleGroup not found on EXIT: $scheduleGroupId")
                            return@forEach
                        }
                        
                        val overrideState = scheduleGroup.manualOverrideState
                        
                        // v8: INACTIVE 상태에서 이탈 시 -> 상태 유지 (사용자 명시적 비활성화 존중)
                        // 사용자가 "끄기"를 선택했다면, 위치를 벗어났다가 다시 돌아와도 계속 꺼져 있어야 함.
                        if (overrideState == "INACTIVE") {
                             Log.i(TAG, "ℹ️ v8: Location EXIT but ScheduleGroup manually INACTIVE (Processing Skipped): $scheduleGroupId")
                             return@forEach
                        }
                        
                        Log.i(TAG, "🚪 Location has linked ScheduleGroup: $scheduleGroupId")
                        
                        // 2. ScheduleGroup 비활성화 (알람 자동 취소)
                        // v8: 위치 이탈로 인한 자동 비활성화이므로 isUserAction=false
                        val result = scheduleManager.deactivateGroup(scheduleGroupId, isUserAction = false)
                        
                        if (result.isSuccess) {
                            Log.i(TAG, "✅ ScheduleGroup deactivated on EXIT: $scheduleGroupId")
                            anyScheduleDeactivated = true
                            
                            // v8: 위치 이탈 시 manualOverrideState는 유지 (사용자 의도 존중)
                            // PAUSED 상태였다면 PAUSED 유지, null이었다면 null 유지
                            Log.d(TAG, "ℹ️ v8: manualOverrideState preserved on EXIT: $overrideState")
                            
                            // TODO: Week 3 - 시간표 비활성화 알림 표시
                            // showScheduleDeactivatedNotification(context, location.label, scheduleGroupId)
                        } else {
                            Log.e(TAG, "⚠️ Failed to deactivate ScheduleGroup: ${result.exceptionOrNull()?.message}")
                        }
                    } else {
                        Log.d(TAG, "ℹ️ No linked ScheduleGroup or deactivation disabled for location: $locationId")
                    }
                }
                
                // 🆕 Phase 4: "어디서나 적용" 스케줄 활성화
                // 위치 기반 스케줄이 비활성화된 경우에만 평가
                if (anyScheduleDeactivated) {
                    Log.i(TAG, "🔄 Evaluating 'anywhere' schedule after location exit")
                    
                    val activationResult = nonLocationScheduleManager.activateDefaultPolicy()
                    
                    if (activationResult.isSuccess) {
                        Log.i(TAG, "✅ 'Anywhere' schedule activated after location exit")
                    } else {
                        Log.w(TAG, "ℹ️ No applicable 'anywhere' schedule: ${activationResult.exceptionOrNull()?.message}")
                    }
                } else {
                    Log.d(TAG, "ℹ️ No schedule deactivated, skipping 'anywhere' schedule evaluation")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ [GEOFENCE_EXIT] Error handling geofence exit: ${e.message}", e)
                Log.e(TAG, "❌ [GEOFENCE_EXIT] Stack trace: ${e.stackTraceToString()}")
            } catch (t: Throwable) {
                // 🔧 v0.10.4: Throwable까지 catch하여 Error도 로깅
                Log.e(TAG, "❌ [GEOFENCE_EXIT] CRITICAL: Throwable caught: ${t.message}", t)
                Log.e(TAG, "❌ [GEOFENCE_EXIT] Stack trace: ${t.stackTraceToString()}")
            } finally {
                val elapsed = System.currentTimeMillis() - startTime
                Log.i(TAG, "✅ [GEOFENCE_EXIT] Coroutine completed (${elapsed}ms total)")
                pendingResult.finish()
            }
        }
    }
    
    /**
     * GPS 정확도를 등급으로 변환
     *
     * @param accuracyMeters GPS 정확도 (미터)
     * @return 정확도 등급 (HIGH/MEDIUM/LOW)
     */
    private fun getAccuracyLevel(accuracyMeters: Float?): String {
        return when {
            accuracyMeters == null -> "UNKNOWN"
            accuracyMeters < 20f -> "HIGH"
            accuracyMeters < 50f -> "MEDIUM"
            else -> "LOW"
        }
    }
}

