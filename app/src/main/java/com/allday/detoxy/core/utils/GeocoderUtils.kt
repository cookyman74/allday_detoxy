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
        val longitude: Double,
        val accuracy: Float? = null
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
     * Address에서 위치 이름 추출 (간략한 이름)
     *
     * 우선순위: locality (시/구) > subLocality (동) > featureName (건물명)
     * 예: "고양시", "일산동구", "일산라페스"
     */
    private fun getLocationName(address: Address): String {
        // 1. 시/구 레벨 이름 우선 (예: "고양시")
        address.locality?.let { return it }
        
        // 2. 동 레벨 이름 (예: "장항동")
        address.subLocality?.let { return it }
        
        // 3. 건물명이나 랜드마크 (예: "일산라페스")
        address.featureName?.let { 
            // featureName이 너무 길면 사용하지 않음 (주소 형태인 경우)
            if (it.length <= 20) return it
        }
        
        // 4. 도로명 (예: "장항대로")
        address.thoroughfare?.let { return it }
        
        return "알 수 없는 위치"
    }

    /**
     * Address에서 전체 주소 추출
     *
     * 한국 주소 형태: "시/도 시/구 동 도로명 번지 건물명"
     * 예: "경기도 고양시 일산동구 장항동 장항대로 761번지 일산라페스"
     */
    private fun getFullAddress(address: Address): String {
        val parts = mutableListOf<String>()

        // 1. 시/도 (예: "경기도")
        address.adminArea?.let { parts.add(it) }

        // 2. 시/구 (예: "고양시")
        address.locality?.let { parts.add(it) }

        // 3. 동 (예: "일산동구", "장항동")
        address.subLocality?.let { parts.add(it) }

        // 4. 도로명 (예: "장항대로")
        address.thoroughfare?.let { parts.add(it) }

        // 5. 번지 (예: "761번지")
        address.subThoroughfare?.let { parts.add(it) }

        // 6. 건물명/상세주소 (예: "일산라페스")
        address.featureName?.let { 
            // featureName이 이미 다른 필드와 중복되지 않으면 추가
            if (!parts.any { part -> part.contains(it) }) {
                parts.add(it)
            }
        }

        return parts.joinToString(" ")
    }
}

