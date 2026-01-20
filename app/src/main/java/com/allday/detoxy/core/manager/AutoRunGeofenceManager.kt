package com.allday.detoxy.core.manager

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.allday.detoxy.data.local.dao.LocationBasedAutoRunDao
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.receiver.GeofenceTransitionsReceiver
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 위치 기반 자동 실행을 위한 Geofencing 관리 클래스
 *
 * 주요 기능:
 * - Geofence 등록/해제
 * - Google Play Services 체크
 * - 위치 서비스 상태 체크
 * - 위치 권한 체크
 * - GPS 정확도 로깅
 *
 * 제한사항:
 * - 최대 5개 Geofence 등록 가능 (시스템 제한 100개, 배터리 효율성 고려)
 * - 반경: 50~500m
 * - 트리거: ENTER (진입 시)
 *
 * @property context Application Context
 * @property geofencingClient Google Play Services Geofencing Client
 * @property locationBasedAutoRunDao LocationBasedAutoRun DAO (개수 체크, 전체 해제)
 */
@Singleton
class AutoRunGeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geofencingClient: GeofencingClient,
    private val locationBasedAutoRunDao: LocationBasedAutoRunDao
) {
    companion object {
        private const val TAG = "AutoRunGeofenceManager"
        
        // Geofence 제한
        const val MAX_GEOFENCES = 5 // 배터리 효율성을 위해 5개로 제한
        
        // PendingIntent Action
        const val ACTION_GEOFENCE_TRANSITION = "com.allday.detoxy.ACTION_GEOFENCE_TRANSITION"
        
        // Extra Keys
        const val EXTRA_LOCATION_ID = "locationId"
        const val EXTRA_LOCATION_LABEL = "locationLabel"
        const val EXTRA_DURATION_MINUTES = "durationMinutes"
        const val EXTRA_PRESET_TYPE = "presetType"
        const val EXTRA_REQUIRES_CONFIRMATION = "requiresConfirmation"
        const val EXTRA_DWELL_TIME_MINUTES = "dwellTimeMinutes"
    }
    
    /**
     * Google Play Services 사용 가능 여부 확인
     *
     * Huawei 기기 등 Play Services 미탑재 기기에서는 위치 기반 기능 사용 불가
     *
     * @return true: Play Services 사용 가능, false: 미탑재 또는 구버전
     */
    fun isPlayServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(context)
        
        return when (resultCode) {
            ConnectionResult.SUCCESS -> {
                Log.i(TAG, "✅ Google Play Services is available")
                true
            }
            else -> {
                Log.w(TAG, "⚠️ Google Play Services is NOT available: $resultCode")
                false
            }
        }
    }
    
    /**
     * 위치 서비스 활성화 여부 확인
     *
     * @return true: 위치 서비스 ON, false: 위치 서비스 OFF
     */
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.also { isEnabled ->
            if (isEnabled) {
                Log.i(TAG, "✅ Location service is enabled")
            } else {
                Log.w(TAG, "⚠️ Location service is disabled")
            }
        }
    }
    
    /**
     * 위치 권한 보유 여부 확인
     *
     * @return true: 위치 권한 있음, false: 위치 권한 없음
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 백그라운드 위치 권한 보유 여부 확인 (Android 10+)
     *
     * @return true: 백그라운드 위치 권한 있음, false: 권한 없음 또는 Android 10 미만
     */
    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 10 미만에서는 백그라운드 권한 불필요
        }
    }
    
    /**
     * Geofence 등록
     *
     * @param autoRun 위치 기반 자동 실행 설정
     * @param skipCountCheck true: 개수 제한 체크 생략 (reschedule 시), false: 개수 제한 체크 수행 (신규 등록 시)
     * @return Result<Unit> 성공 시 Success, 실패 시 Failure with Exception
     */
    suspend fun addGeofence(autoRun: LocationBasedAutoRun, skipCountCheck: Boolean = false): Result<Unit> {
        return try {
            // 사전 조건 체크
            if (!isPlayServicesAvailable()) {
                return Result.failure(
                    GeofenceException("Google Play Services가 설치되어 있지 않습니다. 위치 기반 자동 실행을 사용할 수 없습니다.")
                )
            }
            
            if (!isLocationEnabled()) {
                return Result.failure(
                    GeofenceException("위치 서비스가 꺼져 있습니다. 설정에서 위치 서비스를 켜주세요.")
                )
            }
            
            if (!hasLocationPermission()) {
                return Result.failure(
                    GeofenceException("위치 권한이 없습니다. 앱 설정에서 위치 권한을 허용해주세요.")
                )
            }
            
            if (!hasBackgroundLocationPermission()) {
                return Result.failure(
                    GeofenceException("백그라운드 위치 권한이 없습니다. 앱 설정에서 '항상 허용'을 선택해주세요.")
                )
            }
            
            // 현재 등록된 Geofence 수 체크 (자기 자신 제외)
            // skipCountCheck가 true면 reschedule 상황이므로 체크 생략
            if (!skipCountCheck) {
                val otherEnabledCount = locationBasedAutoRunDao.getEnabledCountExcept(autoRun.id)
                
                // 자기 자신을 제외한 활성화된 개수가 MAX_GEOFENCES 이상이면 제한
                if (otherEnabledCount >= MAX_GEOFENCES) {
                    Log.w(TAG, "⚠️ Max geofences limit reached: Others=$otherEnabledCount, MAX=$MAX_GEOFENCES")
                    return Result.failure(
                        GeofenceException("최대 ${MAX_GEOFENCES}개까지만 등록할 수 있습니다. 기존 위치를 삭제한 후 다시 시도해주세요.")
                    )
                }
            }
            
            // Geofence 생성
            val geofence = createGeofence(autoRun)
            val geofencingRequest = createGeofencingRequest(listOf(geofence))
            val pendingIntent = createPendingIntent(autoRun)
            
            // Geofence 등록
            geofencingClient.addGeofences(geofencingRequest, pendingIntent).await()
            
            Log.i(TAG, "✅ Geofence added successfully for ${autoRun.label} (ID: ${autoRun.id})")
            Result.success(Unit)
            
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Security exception while adding geofence: ${e.message}")
            Result.failure(GeofenceException("위치 권한이 없습니다: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to add geofence: ${e.message}")
            Result.failure(GeofenceException("Geofence 등록 실패: ${e.message}"))
        }
    }
    
    /**
     * Geofence 해제
     *
     * @param autoRunId 위치 기반 자동 실행 ID
     * @return Result<Unit> 성공 시 Success, 실패 시 Failure with Exception
     */
    suspend fun removeGeofence(autoRunId: String): Result<Unit> {
        return try {
            geofencingClient.removeGeofences(listOf(autoRunId)).await()
            Log.i(TAG, "✅ Geofence removed successfully for ID: $autoRunId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to remove geofence: ${e.message}")
            Result.failure(GeofenceException("Geofence 해제 실패: ${e.message}"))
        }
    }
    
    /**
     * 모든 Geofence 해제
     *
     * DB에서 활성화된 모든 LocationBasedAutoRun ID를 조회하여 일괄 제거합니다.
     *
     * @return Result<Unit> 성공 시 Success, 실패 시 Failure with Exception
     */
    suspend fun removeAllGeofences(): Result<Unit> {
        return try {
            // DB에서 활성화된 모든 ID 조회
            val enabledIds = locationBasedAutoRunDao.getAllEnabledIds()
            
            if (enabledIds.isEmpty()) {
                Log.i(TAG, "ℹ️ No active geofences to remove")
                return Result.success(Unit)
            }
            
            // 일괄 제거
            geofencingClient.removeGeofences(enabledIds).await()
            
            Log.i(TAG, "✅ All geofences removed successfully (Count: ${enabledIds.size})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to remove all geofences: ${e.message}")
            Result.failure(GeofenceException("모든 Geofence 해제 실패: ${e.message}"))
        }
    }
    
    /**
     * rescheduleAll 결과 (v1.1.2 핫픽스)
     * 
     * @property successIds 등록 성공한 ID 목록
     * @property failedPermanentIds 영구적 실패 ID 목록 (권한, Play Services)
     * @property failedTempIds 일시적 실패 ID 목록 (위치 서비스 OFF 등)
     * @property skippedIds 제한 초과로 스킵된 ID 목록
     */
    data class RescheduleResult(
        val successIds: List<String>,
        val failedPermanentIds: List<String>,
        val failedTempIds: List<String>,
        val skippedIds: List<String>
    ) {
        val isEmpty: Boolean
            get() = successIds.isEmpty() && failedPermanentIds.isEmpty() && 
                    failedTempIds.isEmpty() && skippedIds.isEmpty()
    }
    
    /**
     * 모든 활성화된 Geofence 재등록 (v1.1.2 개선)
     *
     * 앱 재시작(BOOT_COMPLETED) 또는 앱 시작 시 호출되어 모든 활성화된 위치 기반 자동 실행의 Geofence를 재등록합니다.
     *
     * ## v1.1.2 핫픽스 변경사항
     * - ⚠️ 모든 enabled ID의 Geofence를 먼저 제거 (스킵된 ID의 잔존 Geofence 방지)
     * - ⚠️ 실패 사유별 분기 (영구적 실패 vs 일시적 실패)
     * - ⚠️ RescheduleResult 반환 (호출측에서 DB 상태 업데이트 가능)
     *
     * 처리 방식:
     * 1. 모든 enabled ID의 Geofence 제거 (잔존 Geofence 방지)
     * 2. 상위 MAX_GEOFENCES개만 재등록 (초과분은 스킵)
     * 3. 실패 사유별 분류 (영구적/일시적)
     *
     * @param enabledLocations 활성화된 위치 기반 자동 실행 리스트
     * @return RescheduleResult 성공/실패/스킵 ID 목록
     */
    suspend fun rescheduleAll(enabledLocations: List<LocationBasedAutoRun>): RescheduleResult {
        if (enabledLocations.isEmpty()) {
            Log.i(TAG, "ℹ️ No enabled locations to reschedule")
            return RescheduleResult(
                successIds = emptyList(),
                failedPermanentIds = emptyList(),
                failedTempIds = emptyList(),
                skippedIds = emptyList()
            )
        }
        
        val successIds = mutableListOf<String>()
        val failedPermanentIds = mutableListOf<String>()
        val failedTempIds = mutableListOf<String>()
        
        // 🆕 v1.1.2: 모든 enabled ID의 Geofence를 먼저 제거 (스킵된 ID의 잔존 방지)
        val allEnabledIds = enabledLocations.map { it.id }
        try {
            Log.d(TAG, "🗑️ Removing ALL ${allEnabledIds.size} enabled geofences before rescheduling")
            geofencingClient.removeGeofences(allEnabledIds).await()
            Log.d(TAG, "✅ All ${allEnabledIds.size} existing geofences removed")
        } catch (e: Exception) {
            // 제거 실패해도 계속 진행 (이미 제거되었거나 등록되지 않았을 수 있음)
            Log.w(TAG, "⚠️ Failed to remove some geofences (may not exist): ${e.message}")
        }
        
        // 상위 MAX_GEOFENCES개만 등록, 나머지는 스킵
        val locationsToRegister = enabledLocations.take(MAX_GEOFENCES)
        val skippedIds = enabledLocations.drop(MAX_GEOFENCES).map { it.id }
        
        if (skippedIds.isNotEmpty()) {
            Log.w(TAG, "⚠️ ${skippedIds.size}개 위치가 제한 초과로 등록 제외됨 (MAX=$MAX_GEOFENCES)")
        }
        
        Log.i(TAG, "🔄 Rescheduling ${locationsToRegister.size} geofences (of ${enabledLocations.size} enabled)")
        
        // 🆕 v1.1.2: 사전 조건 체크 (영구적 vs 일시적 분류용)
        val isPermanentFailure = !isPlayServicesAvailable() || 
                                  !hasLocationPermission() || 
                                  !hasBackgroundLocationPermission()
        
        locationsToRegister.forEach { location ->
            // skipCountCheck=true: reschedule 상황에서는 개수 체크 생략
            val result = addGeofence(location, skipCountCheck = true)
            if (result.isSuccess) {
                successIds.add(location.id)
                Log.d(TAG, "✅ Geofence rescheduled: ${location.label} (ID: ${location.id})")
            } else {
                // 🆕 v1.1.2: 실패 사유 분류
                val error = result.exceptionOrNull()
                if (isPermanentFailure) {
                    failedPermanentIds.add(location.id)
                    Log.w(TAG, "⚠️ Permanent failure for ${location.label}: ${error?.message}")
                } else {
                    failedTempIds.add(location.id)  // 위치 서비스 OFF 등 일시적
                    Log.w(TAG, "⚠️ Temporary failure for ${location.label}: ${error?.message}")
                }
            }
        }
        
        Log.i(TAG, """
            📊 Geofence reschedule completed:
            - Total: ${enabledLocations.size}
            - Success: ${successIds.size}
            - Permanent Failure: ${failedPermanentIds.size}
            - Temporary Failure: ${failedTempIds.size}
            - Skipped (limit): ${skippedIds.size}
        """.trimIndent())
        
        return RescheduleResult(
            successIds = successIds,
            failedPermanentIds = failedPermanentIds,
            failedTempIds = failedTempIds,
            skippedIds = skippedIds
        )
    }
    
    /**
     * Geofence 생성
     *
     * ## 트랜지션 타입 설정 로직 (3차 고도화 확장)
     * - EXIT 트리거 필요 여부: deactivateScheduleOnExit 옵션 확인
     * - dwellTimeMinutes에 따라 ENTER/DWELL 트리거 설정
     *
     * ### 트랜지션 타입 조합
     * 1. **deactivateScheduleOnExit = true + dwellTimeMinutes > 0**
     *    → ENTER | DWELL | EXIT (시간표 자동 활성화/비활성화 + 체류 시간)
     * 2. **deactivateScheduleOnExit = true + dwellTimeMinutes = 0**
     *    → ENTER | EXIT (시간표 자동 활성화/비활성화 + 즉시 트리거)
     * 3. **deactivateScheduleOnExit = false + dwellTimeMinutes > 0**
     *    → ENTER | DWELL (체류 시간만)
     * 4. **deactivateScheduleOnExit = false + dwellTimeMinutes = 0**
     *    → ENTER (즉시 트리거만)
     *
     * @param autoRun 위치 기반 자동 실행 설정
     * @return Geofence
     */
    private fun createGeofence(autoRun: LocationBasedAutoRun): Geofence {
        val builder = Geofence.Builder()
            .setRequestId(autoRun.id)
            .setCircularRegion(
                autoRun.latitude,
                autoRun.longitude,
                autoRun.radiusMeters.toFloat()
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
        
        // 트랜지션 타입 결정 (ENTER, DWELL, EXIT)
        var transitionTypes = Geofence.GEOFENCE_TRANSITION_ENTER
        
        // 체류 시간이 있으면 DWELL 추가
        if (autoRun.dwellTimeMinutes > 0) {
            transitionTypes = transitionTypes or Geofence.GEOFENCE_TRANSITION_DWELL
            builder.setLoiteringDelay(autoRun.dwellTimeMinutes * 60 * 1000) // 분 → 밀리초
            Log.d(TAG, "Geofence: DWELL enabled (${autoRun.dwellTimeMinutes}분 체류)")
        }
        
        // 🆕 3차 고도화: 위치 이탈 시 시간표 비활성화 옵션
        if (autoRun.deactivateScheduleOnExit) {
            transitionTypes = transitionTypes or Geofence.GEOFENCE_TRANSITION_EXIT
            Log.d(TAG, "Geofence: EXIT enabled (시간표 자동 비활성화)")
        }
        
        builder.setTransitionTypes(transitionTypes)
        
        val transitionTypesStr = buildList {
            if (transitionTypes and Geofence.GEOFENCE_TRANSITION_ENTER != 0) add("ENTER")
            if (transitionTypes and Geofence.GEOFENCE_TRANSITION_DWELL != 0) add("DWELL")
            if (transitionTypes and Geofence.GEOFENCE_TRANSITION_EXIT != 0) add("EXIT")
        }.joinToString(" | ")
        
        Log.d(TAG, "Geofence created: $transitionTypesStr")
        
        return builder.build()
    }
    
    /**
     * GeofencingRequest 생성
     *
     * @param geofences Geofence 리스트
     * @return GeofencingRequest
     */
    private fun createGeofencingRequest(geofences: List<Geofence>): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            // 초기 트리거: 현재 위치가 Geofence 내부인 경우 즉시 트리거
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofences)
        }.build()
    }
    
    /**
     * PendingIntent 생성
     *
     * Geofence 트리거 시 GeofenceTransitionsReceiver로 Intent 전달
     *
     * @param autoRun 위치 기반 자동 실행 설정 (null일 경우 기본 Intent)
     * @return PendingIntent
     */
    private fun createPendingIntent(autoRun: LocationBasedAutoRun?): PendingIntent {
        val intent = Intent(context, GeofenceTransitionsReceiver::class.java).apply {
            action = ACTION_GEOFENCE_TRANSITION
            
            autoRun?.let {
                putExtra(EXTRA_LOCATION_ID, it.id)
                putExtra(EXTRA_LOCATION_LABEL, it.label)
                putExtra(EXTRA_DURATION_MINUTES, it.durationMinutes)
                putExtra(EXTRA_PRESET_TYPE, it.presetType)
                putExtra(EXTRA_REQUIRES_CONFIRMATION, it.requiresUserConfirmation)
                putExtra(EXTRA_DWELL_TIME_MINUTES, it.dwellTimeMinutes)
            }
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getBroadcast(
            context,
            autoRun?.id?.hashCode() ?: 0,
            intent,
            flags
        )
    }
    
    /**
     * Geofence 관련 커스텀 예외
     */
    class GeofenceException(message: String) : Exception(message)
}

