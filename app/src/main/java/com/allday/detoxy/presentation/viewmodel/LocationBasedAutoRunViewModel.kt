package com.allday.detoxy.presentation.viewmodel

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.core.manager.AutoRunGeofenceManager
import com.allday.detoxy.core.manager.ScheduleGroupManager
import com.allday.detoxy.core.utils.AnalyticsHelper
import com.allday.detoxy.core.utils.PermissionUtils
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.data.repository.LocationBasedAutoRunRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject

/**
 * 위치 기반 자동 실행 에러 타입
 */
sealed class LocationError {
    data class GeofenceError(val message: String) : LocationError()
    data class PermissionError(val message: String) : LocationError()
    data class DatabaseError(val message: String) : LocationError()
    data class UnknownError(val message: String) : LocationError()
}

/**
 * 위치 기반 자동 실행 ViewModel
 *
 * 위치 기반 자동 실행 설정 화면의 비즈니스 로직을 담당합니다.
 *
 * 주요 기능:
 * - 위치 기반 자동 실행 조회/추가/수정/삭제
 * - Geofence 등록/해제
 * - Google Play Services 상태 체크
 * - 위치 권한 상태 체크
 * - 위치 서비스 상태 체크
 * - 에러 상태 관리
 *
 * @property repository LocationBasedAutoRunRepository
 * @property geofenceManager AutoRunGeofenceManager
 * @property context Application Context
 */
@HiltViewModel
class LocationBasedAutoRunViewModel @Inject constructor(
    private val repository: LocationBasedAutoRunRepository,
    private val geofenceManager: AutoRunGeofenceManager,
    private val scheduleGroupManager: ScheduleGroupManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val TAG = "LocationBasedAutoRunViewModel"
    }

    /**
     * 위치 기반 자동 실행 리스트
     */
    val locations: StateFlow<List<LocationBasedAutoRun>> = repository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Google Play Services 사용 가능 여부
     */
    private val _playServicesAvailable = MutableStateFlow(true)
    val playServicesAvailable: StateFlow<Boolean> = _playServicesAvailable.asStateFlow()

    /**
     * 위치 권한 상태
     */
    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted.asStateFlow()

    /**
     * 백그라운드 위치 권한 상태
     */
    private val _backgroundLocationPermissionGranted = MutableStateFlow(false)
    val backgroundLocationPermissionGranted: StateFlow<Boolean> = _backgroundLocationPermissionGranted.asStateFlow()

    /**
     * 위치 서비스 활성화 여부
     */
    private val _locationServiceEnabled = MutableStateFlow(true)
    val locationServiceEnabled: StateFlow<Boolean> = _locationServiceEnabled.asStateFlow()

    /**
     * 전체 위치 권한 부여 여부 (정확한 위치 + 백그라운드 위치)
     */
    val hasFullLocationPermission: StateFlow<Boolean> = combine(
        _locationPermissionGranted,
        _backgroundLocationPermissionGranted
    ) { location, background ->
        location && background
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /**
     * 에러 상태
     */
    private val _errorState = MutableStateFlow<LocationError?>(null)
    val errorState: StateFlow<LocationError?> = _errorState.asStateFlow()

    // ==================== Phase 3: UX 표시용 활성화 개수 ====================
    
    /**
     * 추가 활성화 가능 여부 (UX 표시용)
     * 
     * ⚠️ Flow 기반으로 일시적인 지연이 있을 수 있음
     * 정책 체크는 getEnabledCount() 직접 호출로 수행
     */
    private val _canActivateMore = MutableStateFlow(true)
    val canActivateMore: StateFlow<Boolean> = _canActivateMore.asStateFlow()
    
    /**
     * 현재 활성화된 위치 개수 (UX 표시용)
     * 
     * UI에서 "활성화: 3/5" 등의 형태로 표시 가능
     */
    private val _enabledCount = MutableStateFlow(0)
    val enabledCount: StateFlow<Int> = _enabledCount.asStateFlow()
    
    /**
     * 최대 활성화 가능 개수
     */
    val maxGeofences: Int = AutoRunGeofenceManager.MAX_GEOFENCES

    init {
        checkPermissions()
        
        // ⚠️ 핫픽스 Phase 3: 활성화 개수 Flow 수집 (UX 표시용)
        viewModelScope.launch {
            repository.getEnabled().collect { list ->
                _enabledCount.value = list.size
                _canActivateMore.value = list.size < AutoRunGeofenceManager.MAX_GEOFENCES
            }
        }
    }

    /**
     * 에러 상태 초기화
     */
    fun clearError() {
        _errorState.value = null
    }

    /**
     * 위치 라벨을 SHA-256으로 해시 처리
     *
     * Analytics 개인정보 보호를 위해 라벨을 해시 처리합니다.
     * 실제 위치 이름 대신 해시값만 로깅됩니다.
     *
     * @param label 위치 라벨 (예: "회사", "도서관")
     * @return SHA-256 해시값 (32자 hex)
     */
    private fun hashLocationLabel(label: String): String {
        val bytes = label.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 권한 상태 확인
     * 
     * 권한 변경 감지 시 Geofence 자동 해제:
     * - 이전에는 전체 권한이 있었는데 현재는 없는 경우 → 모든 Geofence 해제 + DB 비활성화
     * 
     * ⚠️ v1.1.2 핫픽스: removeAllGeofences() 성공 시에만 DB 비활성화 (역불일치 방지)
     */
    fun checkPermissions() {
        val previousHasFullPermission = hasFullLocationPermission.value
        
        _playServicesAvailable.value = geofenceManager.isPlayServicesAvailable()
        _locationPermissionGranted.value = geofenceManager.hasLocationPermission()
        _backgroundLocationPermissionGranted.value = geofenceManager.hasBackgroundLocationPermission()
        _locationServiceEnabled.value = geofenceManager.isLocationEnabled()
        
        val currentHasFullPermission = hasFullLocationPermission.value
        
        // 권한 해제 감지: 이전에는 있었는데 현재는 없음
        if (previousHasFullPermission && !currentHasFullPermission) {
            viewModelScope.launch {
                Log.w(TAG, "⚠️ Location permission revoked")
                
                // 🆕 v1.1.2: Geofence 제거 성공 여부 확인
                val removeResult = geofenceManager.removeAllGeofences()
                
                if (removeResult.isSuccess) {
                    // Geofence 제거 성공 시에만 DB 비활성화
                    repository.disableAll()
                    Log.i(TAG, "✅ All geofences removed and DB disabled")
                } else {
                    // Geofence 제거 실패 시 DB는 그대로 유지 (재시도 유도)
                    Log.w(TAG, "⚠️ Failed to remove geofences, DB not modified: ${removeResult.exceptionOrNull()?.message}")
                }
                
                // 사용자에게 알림
                _errorState.value = LocationError.PermissionError(
                    "위치 권한이 해제되어 위치 기반 자동 실행이 비활성화되었습니다."
                )
            }
        }
    }

    /**
     * 위치 기반 자동 실행 추가
     *
     * 트랜잭션 순서:
     * 1. Geofence 등록 (활성화된 경우만) - 실패 시 isEnabled=false로 DB 저장
     * 2. DB 저장 - 실패 시 Geofence 롤백
     * 3. Analytics 로깅
     *
     * @param location 추가할 위치 기반 자동 실행
     */
    suspend fun addLocation(location: LocationBasedAutoRun) {
        try {
            Log.d(TAG, "🔵 addLocation called: label=${location.label}, linkedScheduleGroupId=${location.linkedScheduleGroupId}")
            Log.d(TAG, "   isEnabled=${location.isEnabled}, latitude=${location.latitude}, longitude=${location.longitude}")
            
            // ⚠️ 핫픽스: 활성화된 상태로 등록 시 선제 개수 체크 (UX 빠른 피드백)
            if (location.isEnabled) {
                val currentEnabledCount = repository.getEnabledCount()
                if (currentEnabledCount >= AutoRunGeofenceManager.MAX_GEOFENCES) {
                    Log.w(TAG, "⚠️ Pre-check: Max geofences limit reached ($currentEnabledCount >= ${AutoRunGeofenceManager.MAX_GEOFENCES})")
                    _errorState.value = LocationError.GeofenceError(
                        "최대 ${AutoRunGeofenceManager.MAX_GEOFENCES}개까지만 활성화할 수 있습니다."
                    )
                    return
                }
            }
            
            // 1. Geofence 등록 먼저 시도 (활성화된 경우만)
            if (location.isEnabled) {
                Log.d(TAG, "📍 Attempting to add Geofence...")
                val result = geofenceManager.addGeofence(location)
                if (result.isFailure) {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "❌ Geofence registration FAILED: ${exception?.message}", exception)
                    
                    // ⚠️ 핫픽스: Geofence 실패 시 isEnabled=false로 저장
                    val disabledLocation = location.copy(isEnabled = false)
                    repository.insert(disabledLocation)
                    Log.w(TAG, "⚠️ Location saved with isEnabled=false due to Geofence failure: ${location.id}")
                    
                    _errorState.value = LocationError.GeofenceError(
                        "위치가 등록되었지만 모니터링 활성화에 실패했습니다.\n" +
                        "설정에서 다시 활성화를 시도해주세요."
                    )
                    return
                }
                Log.d(TAG, "✅ Geofence registered successfully")
            } else {
                Log.d(TAG, "ℹ️ Skipping Geofence registration (isEnabled=false)")
            }

            // 2. Geofence 성공 후 DB 저장
            Log.d(TAG, "💾 Saving location to DB...")
            repository.insert(location)
            Log.d(TAG, "✅ Location saved to DB successfully: ${location.id}")
            
            // 🆕 Geofence 등록 후 현재 위치 확인 및 즉시 활성화 (DB 저장 완료 후 지연 실행)
            // 위치 접근이 백그라운드 포그라운드 서비스 시작과 충돌하지 않도록 지연 처리
            if (location.isEnabled && location.activateScheduleOnEnter && location.linkedScheduleGroupId != null) {
                viewModelScope.launch {
                    // 2초 지연하여 포그라운드 서비스가 완전히 시작된 후 위치 접근
                    delay(2000)
                    checkAndActivateLocationIfInside(location)
                }
            }

            // 3. Analytics 로깅
            AnalyticsHelper.logLocationBasedAutoRunCreated(
                locationLabelHash = hashLocationLabel(location.label),
                radiusMeters = location.radiusMeters,
                durationMinutes = location.durationMinutes,
                presetType = location.presetType,
                triggerType = location.triggerType,
                dwellTimeMinutes = location.dwellTimeMinutes,
                requiresConfirmation = location.requiresUserConfirmation
            )
            Log.d(TAG, "📊 Analytics: location_created (label_hash: ${hashLocationLabel(location.label).take(8)}...)")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in addLocation: ${e.message}", e)
            // Geofence는 등록되었지만 DB 저장 실패 → Geofence 롤백
            if (location.isEnabled) {
                geofenceManager.removeGeofence(location.id)
            }
            _errorState.value = LocationError.DatabaseError(
                "저장 실패: ${e.message ?: "알 수 없는 오류"}"
            )
        }
    }

    /**
     * 위치 정보 업데이트 (suspend 함수)
     *
     * Geofence 재등록 및 DB 업데이트를 순차적으로 처리합니다.
     * suspend 함수로 구현되어 호출자가 완료를 기다릴 수 있습니다.
     * 
     * 트랜잭션 순서:
     * 1. 기존 Geofence 제거
     * 2. 새 Geofence 등록 (활성화된 경우만) - 실패 시 isEnabled=false로 DB 업데이트
     * 3. DB 업데이트 - 실패 시 Geofence 롤백
     *
     * @param location 업데이트할 위치 기반 자동 실행
     */
    suspend fun updateLocation(location: LocationBasedAutoRun) {
        try {
            Log.d(TAG, "🔵 updateLocation called: id=${location.id}, isEnabled=${location.isEnabled}")
            Log.d(TAG, "   activateScheduleOnEnter=${location.activateScheduleOnEnter}, deactivateScheduleOnExit=${location.deactivateScheduleOnExit}")
            
            // ⚠️ 핫픽스: 활성화 상태로 업데이트 시 선제 개수 체크 (자기 자신 제외)
            // 기존에 비활성화였다가 활성화로 변경하는 경우를 위한 체크
            if (location.isEnabled) {
                val currentEnabledCount = repository.getEnabledCount()
                // 자기 자신이 이미 활성화 상태면 카운트에서 1을 빼야 함
                val existing = repository.getById(location.id).first()
                val adjustedCount = maxOf(0, if (existing?.isEnabled == true) currentEnabledCount - 1 else currentEnabledCount)
                
                if (adjustedCount >= AutoRunGeofenceManager.MAX_GEOFENCES) {
                    Log.w(TAG, "⚠️ Pre-check: Max geofences limit reached (adjusted: $adjustedCount >= ${AutoRunGeofenceManager.MAX_GEOFENCES})")
                    _errorState.value = LocationError.GeofenceError(
                        "최대 ${AutoRunGeofenceManager.MAX_GEOFENCES}개까지만 활성화할 수 있습니다."
                    )
                    return
                }
            }
            
            // 1. 기존 Geofence 제거
            geofenceManager.removeGeofence(location.id)

            // 2. 새 Geofence 등록 (활성화된 경우만)
            if (location.isEnabled) {
                Log.d(TAG, "📍 Attempting to add Geofence...")
                val result = geofenceManager.addGeofence(location)
                if (result.isFailure) {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "❌ Geofence registration FAILED: ${exception?.message}", exception)
                    
                    // ⚠️ 핫픽스: Geofence 실패 시 isEnabled=false로 저장
                    val disabledLocation = location.copy(isEnabled = false)
                    repository.update(disabledLocation)
                    Log.w(TAG, "⚠️ Location updated with isEnabled=false due to Geofence failure: ${location.id}")
                    
                    _errorState.value = LocationError.GeofenceError(
                        "위치가 업데이트되었지만 모니터링 활성화에 실패했습니다.\n" +
                        "설정에서 다시 활성화를 시도해주세요."
                    )
                    return
                }
                Log.d(TAG, "✅ Geofence registered successfully")
            } else {
                Log.d(TAG, "ℹ️ Skipping Geofence registration (isEnabled=false)")
            }

            // 3. Geofence 성공 후 DB 업데이트
            Log.d(TAG, "💾 Updating location in DB...")
            repository.update(location)
            Log.d(TAG, "✅ Location updated in DB successfully: ${location.id}")
            Log.d(TAG, "   Final state: activateScheduleOnEnter=${location.activateScheduleOnEnter}, deactivateScheduleOnExit=${location.deactivateScheduleOnExit}")
            
            // 🆕 Geofence 등록 후 현재 위치 확인 및 즉시 활성화 (DB 업데이트 완료 후 지연 실행)
            // 위치 접근이 백그라운드 포그라운드 서비스 시작과 충돌하지 않도록 지연 처리
            if (location.isEnabled && location.activateScheduleOnEnter && location.linkedScheduleGroupId != null) {
                viewModelScope.launch {
                    // 2초 지연하여 포그라운드 서비스가 완전히 시작된 후 위치 접근
                    delay(2000)
                    checkAndActivateLocationIfInside(location)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in updateLocation: ${e.message}", e)
            // DB 업데이트 실패 → Geofence 롤백 (제거)
            geofenceManager.removeGeofence(location.id)
            _errorState.value = LocationError.DatabaseError(
                "업데이트 실패: ${e.message ?: "알 수 없는 오류"}"
            )
        }
    }

    /**
     * 위치 기반 자동 실행 삭제
     *
     * 트랜잭션 순서:
     * 1. 통계 조회 (Analytics용)
     * 2. DB에서 삭제
     * 3. Geofence 해제 - 실패해도 치명적이지 않음 (이미 DB 삭제됨)
     * 4. Analytics 로깅
     *
     * @param locationId 삭제할 위치 기반 자동 실행 ID
     */
    fun deleteLocation(locationId: String) {
        viewModelScope.launch {
            try {
                // 1. 삭제 전 통계 조회 (Analytics용)
                val location = repository.getById(locationId).first()
                val usageCount = 0 // TODO: AutoRunLog에서 조회 (3차 고도화)
                val successRate = 0 // TODO: AutoRunLog에서 계산 (3차 고도화)
                val daysActive = if (location != null) {
                    val days = (System.currentTimeMillis() - location.createdAt) / (1000 * 60 * 60 * 24)
                    days.toInt()
                } else {
                    0
                }

                // 2. DB에서 먼저 삭제
                repository.deleteById(locationId)

                // 3. Geofence 해제 (실패해도 치명적이지 않음)
                geofenceManager.removeGeofence(locationId)

                // 4. Analytics 로깅
                AnalyticsHelper.logLocationBasedAutoRunDeleted(
                    usageCount = usageCount,
                    successRate = successRate,
                    daysActive = daysActive
                )
                Log.d(TAG, "📊 Analytics: location_deleted (days_active: $daysActive)")

            } catch (e: Exception) {
                _errorState.value = LocationError.DatabaseError(
                    "삭제 실패: ${e.message ?: "알 수 없는 오류"}"
                )
            }
        }
    }

    /**
     * 현재 위치 확인 및 Geofence 내부에 있으면 즉시 스케줄 활성화
     * 
     * Geofence 등록 후 호출되어, 현재 위치가 Geofence 반경 내부에 있으면
     * setInitialTrigger가 작동하지 않는 경우를 대비해 수동으로 활성화합니다.
     * 
     * ⚠️ 위치 접근은 메인 스레드에서 처리하여 백그라운드 포그라운드 서비스와의 충돌을 방지합니다.
     * 
     * @param location 위치 기반 자동 실행 설정
     */
    private fun checkAndActivateLocationIfInside(location: LocationBasedAutoRun) {
        viewModelScope.launch {
            try {
                if (!geofenceManager.hasLocationPermission()) {
                    Log.d(TAG, "⚠️ No location permission, skipping position check")
                    return@launch
                }
                
                Log.d(TAG, "📍 Checking current location for immediate activation...")
                
                // 🆕 메인 스레드에서 위치 접근하여 백그라운드 포그라운드 서비스와의 충돌 방지
                val currentLocation = withContext(Dispatchers.Main) {
                    val cancellationTokenSource = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    ).await()
                }
                
                if (currentLocation == null) {
                    Log.w(TAG, "⚠️ Current location is null, cannot check geofence")
                    return@launch
                }
                
                Log.d(TAG, "📍 Current location: (${currentLocation.latitude}, ${currentLocation.longitude}), accuracy: ${currentLocation.accuracy}m")
                Log.d(TAG, "📍 Geofence center: (${location.latitude}, ${location.longitude}), radius: ${location.radiusMeters}m")
                
                // Geofence 반경 내부인지 확인
                val distance = FloatArray(1)
                Location.distanceBetween(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    location.latitude,
                    location.longitude,
                    distance
                )
                
                val distanceMeters = distance[0].toInt()
                Log.d(TAG, "📏 Distance from geofence center: ${distanceMeters}m")
                
                // GPS 정확도를 고려한 여유분 추가 (정확도 + 50m)
                val effectiveRadius = location.radiusMeters + currentLocation.accuracy.toInt() + 50
                
                if (distanceMeters <= effectiveRadius) {
                    Log.i(TAG, "✅ Current location is INSIDE geofence! Activating schedule immediately...")
                    
                    // ScheduleGroup 활성화
                    val result = scheduleGroupManager.activateGroupByLocation(
                        currentLocation = currentLocation,
                        candidates = listOf(location)
                    )
                    
                    if (result.isSuccess) {
                        Log.i(TAG, "✅ Location-based schedule activated immediately (current location inside geofence)")
                    } else {
                        Log.e(TAG, "⚠️ Failed to activate schedule: ${result.exceptionOrNull()?.message}")
                    }
                } else {
                    Log.d(TAG, "ℹ️ Current location is OUTSIDE geofence (distance: ${distanceMeters}m > effective radius: ${effectiveRadius}m)")
                }
                
            } catch (e: Exception) {
                // 위치 확인 실패는 치명적이지 않음 (Geofence는 정상 작동)
                Log.w(TAG, "⚠️ Failed to check current location for immediate activation: ${e.message}")
            }
        }
    }
    
    /**
     * 위치 기반 자동 실행 활성화/비활성화 토글
     *
     * 트랜잭션 순서:
     * - 활성화: 1) Geofence 등록 → 2) DB 토글 (실패 시 Geofence 롤백) → 3) Analytics 로깅
     * - 비활성화: 1) DB 토글 → 2) Geofence 해제 → 3) Analytics 로깅
     *
     * @param locationId 토글할 위치 기반 자동 실행 ID
     * @param isEnabled 활성화 여부
     */
    fun toggleLocation(locationId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                if (isEnabled) {
                    // ⚠️ 핫픽스: 활성화 시도 시 선제 개수 체크 (자기 자신 제외 보정)
                    // UI와 DB 상태가 일시적으로 불일치하거나, 동일 상태 토글 요청이 들어올 수 있음
                    val location = repository.getById(locationId).first()
                    if (location == null) {
                        Log.w(TAG, "⚠️ Toggle: Location not found: $locationId")
                        return@launch
                    }
                    
                    val currentEnabledCount = repository.getEnabledCount()
                    // 자기 자신이 이미 활성화 상태면 카운트에서 제외해야 함
                    val adjustedCount = maxOf(0, if (location.isEnabled) currentEnabledCount - 1 else currentEnabledCount)
                    
                    if (adjustedCount >= AutoRunGeofenceManager.MAX_GEOFENCES) {
                        Log.w(TAG, "⚠️ Toggle pre-check: Max geofences limit reached (adjusted: $adjustedCount >= ${AutoRunGeofenceManager.MAX_GEOFENCES})")
                        _errorState.value = LocationError.GeofenceError(
                            "최대 ${AutoRunGeofenceManager.MAX_GEOFENCES}개까지만 활성화할 수 있습니다."
                        )
                        return@launch
                    }
                    
                    // 활성화: Geofence 먼저 등록
                    val result = geofenceManager.addGeofence(location)
                    if (result.isFailure) {
                        val exception = result.exceptionOrNull()
                        Log.e(TAG, "❌ Toggle Geofence registration FAILED: ${exception?.message}")
                        // ⚠️ 핫픽스: Geofence 실패 시 DB 토글 안 함, 에러 표시
                        _errorState.value = LocationError.GeofenceError(
                            "위치 모니터링 활성화에 실패했습니다.\n" +
                            "설정에서 다시 시도해주세요."
                        )
                        return@launch  // DB 토글 안 함
                    }

                    // Geofence 성공 시에만 DB 토글
                    repository.toggleEnabled(locationId, isEnabled)

                } else {
                    // 비활성화: DB 먼저 토글
                    repository.toggleEnabled(locationId, isEnabled)

                    // Geofence 해제 (실패해도 치명적이지 않음)
                    geofenceManager.removeGeofence(locationId)
                }

                // Analytics 로깅 (토글 전후 값 보정)
                // 토글 전 상태에서 +1 (활성화) 또는 -1 (비활성화)하여 토글 후 상태 계산
                val currentEnabledCount = locations.value.count { it.isEnabled }
                val totalEnabledCount = if (isEnabled) {
                    currentEnabledCount + 1  // 활성화: 현재 + 1
                } else {
                    currentEnabledCount - 1  // 비활성화: 현재 - 1
                }
                AnalyticsHelper.logLocationBasedAutoRunToggled(
                    isEnabled = isEnabled,
                    totalEnabledCount = totalEnabledCount
                )
                Log.d(TAG, "📊 Analytics: location_toggled (enabled: $isEnabled, total: $totalEnabledCount)")

            } catch (e: Exception) {
                // Geofence는 등록되었지만 DB 토글 실패 → Geofence 롤백
                if (isEnabled) {
                    geofenceManager.removeGeofence(locationId)
                }
                _errorState.value = LocationError.DatabaseError(
                    "토글 실패: ${e.message ?: "알 수 없는 오류"}"
                )
            }
        }
    }
}

