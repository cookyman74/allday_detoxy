package com.allday.detoxy.core.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Geocoder 유틸리티
 *
 * 주소 검색, 좌표 변환 등의 기능을 제공합니다.
 */
object GeocoderUtils {

    private const val TAG = "GeocoderUtils"
    private const val MAX_RESULTS = 5

    /**
     * 위치 정보 데이터 클래스
     */
    data class LocationInfo(
        val name: String,
        val address: String,
        val latitude: Double,
        val longitude: Double
    )

    /**
     * 주소로 위치 검색
     *
     * @param context Android Context
     * @param query 검색 쿼리 (주소 또는 장소 이름)
     * @return 검색된 위치 리스트
     */
    suspend fun searchLocation(
        context: Context,
        query: String
    ): Result<List<LocationInfo>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            val geocoder = Geocoder(context)
            
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ (API 33+): 새로운 비동기 API 사용
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(query, MAX_RESULTS) { addresses ->
                        continuation.resume(addresses)
                    }
                }
            } else {
                // Android 13 미만: 레거시 동기 API 사용
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, MAX_RESULTS) ?: emptyList()
            }

            val results = addresses.mapNotNull { address ->
                try {
                    LocationInfo(
                        name = getLocationName(address),
                        address = getFullAddress(address),
                        latitude = address.latitude,
                        longitude = address.longitude
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing address: ${e.message}", e)
                    null
                }
            }

            Log.d(TAG, "Found ${results.size} locations for query: $query")
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching location: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 좌표로 주소 조회 (Reverse Geocoding)
     *
     * @param context Android Context
     * @param latitude 위도
     * @param longitude 경도
     * @return 주소 정보
     */
    suspend fun getAddressFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Result<LocationInfo> = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context)
            
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ (API 33+): 새로운 비동기 API 사용
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        continuation.resume(addresses)
                    }
                }
            } else {
                // Android 13 미만: 레거시 동기 API 사용
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1) ?: emptyList()
            }

            val address = addresses.firstOrNull()
            if (address != null) {
                val locationInfo = LocationInfo(
                    name = getLocationName(address),
                    address = getFullAddress(address),
                    latitude = latitude,
                    longitude = longitude
                )
                Log.d(TAG, "Reverse geocoding success: ${locationInfo.name}")
                Result.success(locationInfo)
            } else {
                Log.w(TAG, "No address found for coordinates: $latitude, $longitude")
                Result.failure(Exception("주소를 찾을 수 없습니다"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reverse geocoding: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Address에서 위치 이름 추출
     *
     * 우선순위: featureName > thoroughfare > subLocality > locality
     */
    private fun getLocationName(address: Address): String {
        return address.featureName
            ?: address.thoroughfare
            ?: address.subLocality
            ?: address.locality
            ?: "알 수 없는 위치"
    }

    /**
     * Address에서 전체 주소 추출
     */
    private fun getFullAddress(address: Address): String {
        val parts = mutableListOf<String>()

        // 상세 주소
        address.thoroughfare?.let { parts.add(it) }
        address.subThoroughfare?.let { parts.add(it) }

        // 지역
        address.subLocality?.let { parts.add(it) }
        address.locality?.let { parts.add(it) }

        // 시/도
        address.adminArea?.let { parts.add(it) }

        // 국가
        address.countryName?.let { parts.add(it) }

        return parts.joinToString(" ")
    }
}

