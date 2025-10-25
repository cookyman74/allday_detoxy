package com.allday.detoxy.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.core.manager.AutoRunGeofenceManager
import com.allday.detoxy.core.utils.PermissionUtils
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.data.repository.LocationBasedAutoRunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    @ApplicationContext private val context: Context
) : ViewModel() {

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

    init {
        checkPermissions()
    }

    /**
     * 에러 상태 초기화
     */
    fun clearError() {
        _errorState.value = null
    }

    /**
     * 권한 상태 확인
     * 
     * 권한 변경 감지 시 Geofence 자동 해제:
     * - 이전에는 전체 권한이 있었는데 현재는 없는 경우 → 모든 Geofence 해제
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
                Log.w(TAG, "⚠️ Location permission revoked, removing all geofences")
                geofenceManager.removeAllGeofences()
                
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
     * 1. Geofence 등록 (활성화된 경우만) - 실패 시 DB 저장 안 함
     * 2. DB 저장 - 실패 시 Geofence 롤백
     *
     * @param location 추가할 위치 기반 자동 실행
     */
    fun addLocation(location: LocationBasedAutoRun) {
        viewModelScope.launch {
            try {
                // 1. Geofence 등록 먼저 시도 (활성화된 경우만)
                if (location.isEnabled) {
                    val result = geofenceManager.addGeofence(location)
                    if (result.isFailure) {
                        val exception = result.exceptionOrNull()
                        _errorState.value = LocationError.GeofenceError(
                            exception?.message ?: "Geofence 등록 실패. 위치 권한과 Play Services를 확인해주세요."
                        )
                        return@launch  // 실패 시 DB 저장 안 함
                    }
                }

                // 2. Geofence 성공 후 DB 저장
                repository.insert(location)

            } catch (e: Exception) {
                // Geofence는 등록되었지만 DB 저장 실패 → Geofence 롤백
                if (location.isEnabled) {
                    geofenceManager.removeGeofence(location.id)
                }
                _errorState.value = LocationError.DatabaseError(
                    "저장 실패: ${e.message ?: "알 수 없는 오류"}"
                )
            }
        }
    }

    /**
     * 위치 기반 자동 실행 업데이트
     *
     * 트랜잭션 순서:
     * 1. 기존 Geofence 제거
     * 2. 새 Geofence 등록 (활성화된 경우만) - 실패 시 DB 업데이트 안 함
     * 3. DB 업데이트 - 실패 시 원래 Geofence 복구 시도
     *
     * @param location 업데이트할 위치 기반 자동 실행
     */
    fun updateLocation(location: LocationBasedAutoRun) {
        viewModelScope.launch {
            try {
                // 1. 기존 Geofence 제거
                geofenceManager.removeGeofence(location.id)

                // 2. 새 Geofence 등록 (활성화된 경우만)
                if (location.isEnabled) {
                    val result = geofenceManager.addGeofence(location)
                    if (result.isFailure) {
                        val exception = result.exceptionOrNull()
                        _errorState.value = LocationError.GeofenceError(
                            exception?.message ?: "Geofence 등록 실패. 위치 권한과 Play Services를 확인해주세요."
                        )
                        return@launch  // 실패 시 DB 업데이트 안 함
                    }
                }

                // 3. Geofence 성공 후 DB 업데이트
                repository.update(location)

            } catch (e: Exception) {
                // DB 업데이트 실패 → Geofence 롤백 (제거)
                geofenceManager.removeGeofence(location.id)
                _errorState.value = LocationError.DatabaseError(
                    "업데이트 실패: ${e.message ?: "알 수 없는 오류"}"
                )
            }
        }
    }

    /**
     * 위치 기반 자동 실행 삭제
     *
     * 트랜잭션 순서:
     * 1. DB에서 삭제
     * 2. Geofence 해제 - 실패해도 치명적이지 않음 (이미 DB 삭제됨)
     *
     * @param locationId 삭제할 위치 기반 자동 실행 ID
     */
    fun deleteLocation(locationId: String) {
        viewModelScope.launch {
            try {
                // 1. DB에서 먼저 삭제
                repository.deleteById(locationId)

                // 2. Geofence 해제 (실패해도 치명적이지 않음)
                geofenceManager.removeGeofence(locationId)

            } catch (e: Exception) {
                _errorState.value = LocationError.DatabaseError(
                    "삭제 실패: ${e.message ?: "알 수 없는 오류"}"
                )
            }
        }
    }

    /**
     * 위치 기반 자동 실행 활성화/비활성화 토글
     *
     * 트랜잭션 순서:
     * - 활성화: 1) Geofence 등록 → 2) DB 토글 (실패 시 Geofence 롤백)
     * - 비활성화: 1) DB 토글 → 2) Geofence 해제
     *
     * @param locationId 토글할 위치 기반 자동 실행 ID
     * @param isEnabled 활성화 여부
     */
    fun toggleLocation(locationId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                if (isEnabled) {
                    // 활성화: Geofence 먼저 등록
                    val location = repository.getById(locationId).first()
                    if (location != null) {
                        val result = geofenceManager.addGeofence(location)
                        if (result.isFailure) {
                            val exception = result.exceptionOrNull()
                            _errorState.value = LocationError.GeofenceError(
                                exception?.message ?: "Geofence 등록 실패. 위치 권한과 Play Services를 확인해주세요."
                            )
                            return@launch  // 실패 시 DB 토글 안 함
                        }
                    }

                    // Geofence 성공 후 DB 토글
                    repository.toggleEnabled(locationId, isEnabled)

                } else {
                    // 비활성화: DB 먼저 토글
                    repository.toggleEnabled(locationId, isEnabled)

                    // Geofence 해제 (실패해도 치명적이지 않음)
                    geofenceManager.removeGeofence(locationId)
                }

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

