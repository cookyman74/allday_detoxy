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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔔 Geofence event received")
        
        // GeofencingEvent 파싱
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        
        if (geofencingEvent == null) {
            Log.e(TAG, "❌ GeofencingEvent is null")
            return
        }
        
        if (geofencingEvent.hasError()) {
            val errorCode = geofencingEvent.errorCode
            Log.e(TAG, "❌ Geofencing error: $errorCode")
            return
        }
        
        // Geofence 트랜지션 타입 확인
        val geofenceTransition = geofencingEvent.geofenceTransition
        
        // ENTER, DWELL, EXIT 이벤트 처리
        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.i(TAG, "📍 Geofence ENTER detected (즉시 진입)")
                handleGeofenceTrigger(context, intent, geofencingEvent, "ENTER")
            }
            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                Log.i(TAG, "⏱️ Geofence DWELL detected (체류 시간 도달)")
                handleGeofenceTrigger(context, intent, geofencingEvent, "DWELL")
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.i(TAG, "🚪 Geofence EXIT detected (위치 이탈)")
                handleGeofenceExit(context, intent, geofencingEvent)
            }
            else -> {
                Log.w(TAG, "⚠️ Unexpected geofence transition: $geofenceTransition")
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
        intent: Intent,
        geofencingEvent: GeofencingEvent,
        transitionType: String
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
                
                // 비동기 작업 (goAsync)
                val pendingResult = goAsync()
                
        scope.launch {
            try {
                // 🆕 Phase 3: 반경 내 모든 위치 수집 (충돌 해소를 위해)
                val locationIds = triggeringGeofences.map { it.requestId }
                val candidates = mutableListOf<com.allday.detoxy.data.local.entity.LocationBasedAutoRun>()
                
                Log.i(TAG, "🔍 Collecting location candidates: ${locationIds.size} geofences triggered")
                
                locationIds.forEach { id ->
                    val location = locationDao.getByIdOnce(id)
                    if (location != null) {
                        Log.d(TAG, "  - ${location.label} (activateOnEnter: ${location.activateScheduleOnEnter}, scheduleGroup: ${location.linkedScheduleGroupId})")
                        
                        // activateScheduleOnEnter가 true이고, 시간표가 연결된 위치만 후보에 추가
                        if (location.activateScheduleOnEnter && location.linkedScheduleGroupId != null) {
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
                
                Log.i(TAG, "📊 Location candidates collected: ${candidates.size} eligible")
                
                if (candidates.isEmpty()) {
                    Log.w(TAG, "⚠️ No eligible locations (all disabled or no linked schedule group)")
                    return@launch
                }
                
                // 🔧 Critical Fix: 충돌 해소를 먼저 수행하여 winner 위치 확인
                val triggerTime = System.currentTimeMillis()
                val winner = conflictResolver.resolveConflict(userLocation, candidates)
                
                if (winner == null) {
                    Log.e(TAG, "⚠️ No winner location found after conflict resolution")
                    
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
                    Log.i(TAG, "✅ Location-based schedule activated (with conflict resolution)")
                    
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
                        Log.i(TAG, "✅ AutoRunLog recorded: LOCATION STARTED (locationId=${winner.id}, locationLabel=${winner.label}, gpsAccuracy=${gpsAccuracyMeters}m)")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to record AutoRunLog: ${e.message}", e)
                    }
                    
                    // TODO: Week 3 - 시간표 활성화 알림 표시
                    // showScheduleActivatedNotification(context, winner.label, scheduleGroupId)
                } else {
                    Log.e(TAG, "⚠️ Failed to activate schedule", result.exceptionOrNull())
                    
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
                        Log.i(TAG, "✅ AutoRunLog recorded: LOCATION FAILED (locationId=${winner.id}, locationLabel=${winner.label})")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to record AutoRunLog: ${e.message}", e)
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
                    Log.w(TAG, "⚠️ GPS accuracy is low (${gpsAccuracyMeters}m), might cause issues")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error handling geofence enter (location conflict resolution): ${e.message}", e)
                
                // Analytics: auto_run_failed
                AnalyticsHelper.logAutoRunFailed(
                    triggerType = "LOCATION",
                    failureReason = e.message ?: "exception",
                    gpsAccuracyMeters = gpsAccuracyMeters
                )
            } finally {
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
        
        // 비동기 작업 (goAsync)
        val pendingResult = goAsync()
        
        scope.launch {
            try {
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
                        
                        Log.i(TAG, "🚪 Location has linked ScheduleGroup: $scheduleGroupId")
                        
                        // 2. ScheduleGroup 비활성화 (알람 자동 취소)
                        val result = scheduleManager.deactivateGroup(scheduleGroupId)
                        
                        if (result.isSuccess) {
                            Log.i(TAG, "✅ ScheduleGroup deactivated on EXIT: $scheduleGroupId")
                            anyScheduleDeactivated = true
                            
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
                Log.e(TAG, "❌ Error handling geofence exit: ${e.message}", e)
            } finally {
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

