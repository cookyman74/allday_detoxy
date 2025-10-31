package com.allday.detoxy.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.allday.detoxy.core.manager.AutoRunGeofenceManager
import com.allday.detoxy.core.manager.ScheduleGroupManager
import com.allday.detoxy.core.utils.AnalyticsHelper
import com.allday.detoxy.data.local.dao.LocationBasedAutoRunDao
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
        
        // GPS 정확도 추출 (AutoRunLog에 기록용)
        val gpsAccuracyMeters = geofencingEvent.triggeringLocation?.accuracy
        Log.i(TAG, "📡 GPS Accuracy: ${gpsAccuracyMeters ?: "Unknown"}m")
        
        // 각 Geofence 처리
        triggeringGeofences.forEach { geofence ->
                val locationId = geofence.requestId
                
                // Intent에서 위치 기반 자동 실행 정보 추출
                val locationLabel = intent.getStringExtra(AutoRunGeofenceManager.EXTRA_LOCATION_LABEL)
                val durationMinutes = intent.getIntExtra(AutoRunGeofenceManager.EXTRA_DURATION_MINUTES, 0)
                val presetType = intent.getStringExtra(AutoRunGeofenceManager.EXTRA_PRESET_TYPE)
                val requiresConfirmation = intent.getBooleanExtra(AutoRunGeofenceManager.EXTRA_REQUIRES_CONFIRMATION, false)
                val dwellTimeMinutes = intent.getIntExtra(AutoRunGeofenceManager.EXTRA_DWELL_TIME_MINUTES, 0)
                
                Log.i(TAG, """
                    📍 Geofence triggered ($transitionType):
                    - ID: $locationId
                    - Label: $locationLabel
                    - Duration: ${durationMinutes}min
                    - Preset: $presetType
                    - Requires Confirmation: $requiresConfirmation
                    - Dwell Time: ${dwellTimeMinutes}min
                    - GPS Accuracy: ${gpsAccuracyMeters ?: "Unknown"}m
                    - Transition Type: $transitionType
                """.trimIndent())
                
                // Analytics: auto_run_triggered
                AnalyticsHelper.logAutoRunTriggered(
                    triggerType = "LOCATION",
                    sourceIdHash = hashSourceId(locationId),
                    durationMinutes = durationMinutes,
                    presetType = presetType ?: "UNKNOWN"
                )
                Log.d(TAG, "📊 Analytics: auto_run_triggered (LOCATION)")
                
                // 🆕 3차 고도화: ScheduleGroup 활성화 (위치 진입 시)
                // EntryPoint를 통해 필요한 의존성 가져오기
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    GeofenceReceiverEntryPoint::class.java
                )
                
                val locationDao = entryPoint.locationBasedAutoRunDao()
                val scheduleManager = entryPoint.scheduleGroupManager()
                
                // 비동기 작업 (goAsync)
                val pendingResult = goAsync()
                
                scope.launch {
                    try {
                        // 1. locationId로 LocationBasedAutoRun 조회
                        val location = locationDao.getByIdOnce(locationId)
                        
                        if (location != null && location.activateScheduleOnEnter && location.linkedScheduleGroupId != null) {
                            val scheduleGroupId = location.linkedScheduleGroupId
                            
                            Log.i(TAG, "📍 Location has linked ScheduleGroup: $scheduleGroupId")
                            
                            // 2. ScheduleGroup 활성화 (알람 자동 등록)
                            val result = scheduleManager.activateGroup(scheduleGroupId)
                            
                            if (result.isSuccess) {
                                Log.i(TAG, "✅ ScheduleGroup activated on ENTER: $scheduleGroupId")
                                
                                // TODO: Week 3 - 시간표 활성화 알림 표시
                                // showScheduleActivatedNotification(context, location.label, scheduleGroupId)
                            } else {
                                Log.e(TAG, "⚠️ Failed to activate ScheduleGroup: ${result.exceptionOrNull()?.message}")
                            }
                        } else {
                            Log.d(TAG, "ℹ️ No linked ScheduleGroup or activation disabled for location: $locationId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error handling geofence enter (ScheduleGroup activation): ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
                
                // TODO: Week 3 작업 - AutoRunNotificationManager 연동
                // if (requiresConfirmation) {
                //     autoRunNotificationManager.showConfirmationNotification(locationId, locationLabel, durationMinutes)
                //     
                //     // Analytics: auto_run_notification_shown (알림 표시 후)
                //     AnalyticsHelper.logAutoRunNotificationShown(
                //         triggerType = "LOCATION",
                //         isPreNotification = false,
                //         minutesBefore = null
                //     )
                //     Log.d(TAG, "📊 Analytics: auto_run_notification_shown (LOCATION)")
                // } else {
                //     autoRunNotificationManager.showStartNotification(locationId, locationLabel, durationMinutes)
                //     
                //     // Analytics: auto_run_notification_shown (알림 표시 후)
                //     AnalyticsHelper.logAutoRunNotificationShown(
                //         triggerType = "LOCATION",
                //         isPreNotification = false,
                //         minutesBefore = null
                //     )
                //     Log.d(TAG, "📊 Analytics: auto_run_notification_shown (LOCATION)")
                // }
                
                // TODO: Week 3 작업 - AutoRunLog 기록
                // val log = AutoRunLog(
                //     triggerType = "LOCATION",
                //     triggerSourceId = locationId,
                //     triggerTime = System.currentTimeMillis(),
                //     result = "STARTED", // 또는 "SKIPPED" (이미 타이머 실행 중일 때)
                //     failureReason = null,
                //     sessionId = null, // 세션 시작 후 업데이트
                //     gpsAccuracyMeters = gpsAccuracyMeters,
                //     dwellSeconds = dwellTimeMinutes * 60, // 체류 시간 (초)
                //     metaJson = null
                // )
                // autoRunLogDao.insert(log)
                
                // TODO: Week 3 작업 - 이미 타이머 실행 중인지 확인
                // if (timerViewModel.isTimerRunning()) {
                //     Log.w(TAG, "⚠️ Timer is already running, skipping auto-run")
                //     
                //     // Analytics: auto_run_skipped
                //     AnalyticsHelper.logAutoRunSkipped(
                //         triggerType = "LOCATION",
                //         reason = "timer_already_running"
                //     )
                //     return
                // }
                
                // Analytics: auto_run_started (타이머 시작 시)
                // TODO: Week 3 작업 - 실제 타이머 시작 후 호출
                // AnalyticsHelper.logAutoRunStarted(
                //     triggerType = "LOCATION",
                //     durationMinutes = durationMinutes,
                //     isAutoStart = !requiresConfirmation,
                //     delaySeconds = 0,
                //     gpsAccuracyMeters = gpsAccuracyMeters,
                //     dwellSeconds = dwellTimeMinutes * 60
                // )
                // Log.d(TAG, "📊 Analytics: auto_run_started (LOCATION)")
                
                // Analytics: auto_run_failed (GPS 정확도 낮을 때)
                if (gpsAccuracyMeters != null && gpsAccuracyMeters > 100f) {
                    Log.w(TAG, "⚠️ GPS accuracy is low (${gpsAccuracyMeters}m), might cause issues")
                    // TODO: Week 3 작업 - 정확도 낮음 처리 로직
                    // AnalyticsHelper.logAutoRunFailed(
                    //     triggerType = "LOCATION",
                    //     failureReason = "low_gps_accuracy",
                    //     gpsAccuracyMeters = gpsAccuracyMeters
                    // )
                }
                
                Log.i(TAG, "✅ Location-based auto-run triggered for $locationLabel")
        }
    }
    
    /**
     * Geofence EXIT 처리 (위치 이탈)
     *
     * 위치 이탈 시 연결된 ScheduleGroup을 비활성화하고 관련 알람을 취소합니다.
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
        
        // 각 Geofence 처리
        triggeringGeofences.forEach { geofence ->
            val locationId = geofence.requestId
            val locationLabel = intent.getStringExtra(AutoRunGeofenceManager.EXTRA_LOCATION_LABEL)
            
            Log.i(TAG, """
                🚪 Geofence EXIT:
                - ID: $locationId
                - Label: $locationLabel
            """.trimIndent())
            
            // 🆕 3차 고도화: ScheduleGroup 비활성화 (위치 이탈 시)
            // EntryPoint를 통해 필요한 의존성 가져오기
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                GeofenceReceiverEntryPoint::class.java
            )
            
            val locationDao = entryPoint.locationBasedAutoRunDao()
            val scheduleManager = entryPoint.scheduleGroupManager()
            
            // 비동기 작업 (goAsync)
            val pendingResult = goAsync()
            
            scope.launch {
                try {
                    // 1. locationId로 LocationBasedAutoRun 조회
                    val location = locationDao.getByIdOnce(locationId)
                    
                    if (location != null && location.deactivateScheduleOnExit && location.linkedScheduleGroupId != null) {
                        val scheduleGroupId = location.linkedScheduleGroupId
                        
                        Log.i(TAG, "🚪 Location has linked ScheduleGroup: $scheduleGroupId")
                        
                        // 2. ScheduleGroup 비활성화 (알람 자동 취소)
                        val result = scheduleManager.deactivateGroup(scheduleGroupId)
                        
                        if (result.isSuccess) {
                            Log.i(TAG, "✅ ScheduleGroup deactivated on EXIT: $scheduleGroupId")
                            
                            // TODO: Week 3 - 시간표 비활성화 알림 표시
                            // showScheduleDeactivatedNotification(context, location.label, scheduleGroupId)
                        } else {
                            Log.e(TAG, "⚠️ Failed to deactivate ScheduleGroup: ${result.exceptionOrNull()?.message}")
                        }
                    } else {
                        Log.d(TAG, "ℹ️ No linked ScheduleGroup or deactivation disabled for location: $locationId")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error handling geofence exit (ScheduleGroup deactivation): ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
            
            Log.i(TAG, "✅ Geofence EXIT processed for $locationLabel")
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

