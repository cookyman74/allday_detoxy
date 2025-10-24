package com.allday.detoxy.presentation.viewmodel

import android.content.Context
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

    init {
        checkPermissions()
    }

    /**
     * 권한 상태 확인
     */
    fun checkPermissions() {
        _playServicesAvailable.value = geofenceManager.isPlayServicesAvailable()
        _locationPermissionGranted.value = geofenceManager.hasLocationPermission()
        _backgroundLocationPermissionGranted.value = geofenceManager.hasBackgroundLocationPermission()
        _locationServiceEnabled.value = geofenceManager.isLocationEnabled()
    }

    /**
     * 위치 기반 자동 실행 추가
     *
     * @param location 추가할 위치 기반 자동 실행
     */
    fun addLocation(location: LocationBasedAutoRun) {
        viewModelScope.launch {
            try {
                // DB에 저장
                repository.insert(location)

                // Geofence 등록 (활성화된 경우만)
                if (location.isEnabled) {
                    val result = geofenceManager.addGeofence(location)
                    if (result.isFailure) {
                        // TODO: 실패 처리 (사용자에게 알림)
                    }
                }
            } catch (e: Exception) {
                // TODO: 예외 처리
            }
        }
    }

    /**
     * 위치 기반 자동 실행 업데이트
     *
     * @param location 업데이트할 위치 기반 자동 실행
     */
    fun updateLocation(location: LocationBasedAutoRun) {
        viewModelScope.launch {
            try {
                // DB에 업데이트
                repository.update(location)

                // Geofence 재등록
                if (location.isEnabled) {
                    geofenceManager.removeGeofence(location.id)
                    val result = geofenceManager.addGeofence(location)
                    if (result.isFailure) {
                        // TODO: 실패 처리
                    }
                } else {
                    geofenceManager.removeGeofence(location.id)
                }
            } catch (e: Exception) {
                // TODO: 예외 처리
            }
        }
    }

    /**
     * 위치 기반 자동 실행 삭제
     *
     * @param locationId 삭제할 위치 기반 자동 실행 ID
     */
    fun deleteLocation(locationId: String) {
        viewModelScope.launch {
            try {
                // Geofence 해제
                geofenceManager.removeGeofence(locationId)

                // DB에서 삭제
                repository.deleteById(locationId)
            } catch (e: Exception) {
                // TODO: 예외 처리
            }
        }
    }

    /**
     * 위치 기반 자동 실행 활성화/비활성화 토글
     *
     * @param locationId 토글할 위치 기반 자동 실행 ID
     * @param isEnabled 활성화 여부
     */
    fun toggleLocation(locationId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                // DB에 토글
                repository.toggleEnabled(locationId, isEnabled)

                // Geofence 등록/해제
                if (isEnabled) {
                    val location = repository.getById(locationId).first()
                    if (location != null) {
                        val result = geofenceManager.addGeofence(location)
                        if (result.isFailure) {
                            // TODO: 실패 처리
                        }
                    }
                } else {
                    geofenceManager.removeGeofence(locationId)
                }
            } catch (e: Exception) {
                // TODO: 예외 처리
            }
        }
    }
}

