package com.allday.detoxy.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.allday.detoxy.core.manager.AutoRunGeofenceManager

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
    
    companion object {
        private const val TAG = "GeofenceTransitionsReceiver"
    }
    
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
        
        // ENTER 또는 DWELL 이벤트 처리
        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.i(TAG, "📍 Geofence ENTER detected (즉시 진입)")
                handleGeofenceTrigger(context, intent, geofencingEvent, "ENTER")
            }
            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                Log.i(TAG, "⏱️ Geofence DWELL detected (체류 시간 도달)")
                handleGeofenceTrigger(context, intent, geofencingEvent, "DWELL")
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
                
                // TODO: Week 3 작업 - AutoRunNotificationManager 연동
                // if (requiresConfirmation) {
                //     autoRunNotificationManager.showConfirmationNotification(locationId, locationLabel, durationMinutes)
                // } else {
                //     autoRunNotificationManager.showStartNotification(locationId, locationLabel, durationMinutes)
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
                //     return
                // }
                
                Log.i(TAG, "✅ Location-based auto-run triggered for $locationLabel")
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

