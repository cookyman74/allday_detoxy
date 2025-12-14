package com.allday.detoxy.core.utils

import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 위치 관련 유틸리티 (현재 위치 조회 등)
 */
object LocationUtils {
    private const val TAG = "LocationUtils"

    /**
     * 현재 위치를 조회합니다.
     *
     * 1. [Priority.PRIORITY_HIGH_ACCURACY]로 현재 위치 요청
     * 2. [timeoutMs] 내 응답 없으면 TimeoutException 발생 -> getLastLocation 시도
     * 3. 실패 시 Result.failure 반환
     *
     * @param fusedLocationClient FusedLocationProviderClient
     * @param timeoutMs 타임아웃 (기본 5초)
     * @return Result<Location>
     */
    @SuppressLint("MissingPermission") // 호출부에서 권한 체크 필수
    suspend fun getCurrentLocation(
        fusedLocationClient: FusedLocationProviderClient,
        timeoutMs: Long = 5000L
    ): Result<Location> = withContext(Dispatchers.IO) {
        try {
            val location = withTimeout(timeoutMs) {
                suspendCancellableCoroutine<Location> { continuation ->
                    val cancellationTokenSource = CancellationTokenSource()

                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { location ->
                        if (location != null) {
                            Log.d(TAG, "Current location found: ${location.latitude}, ${location.longitude} (acc: ${location.accuracy})")
                            continuation.resume(location)
                        } else {
                            Log.w(TAG, "Current location is null")
                            continuation.resumeWithException(Exception("Current location is null"))
                        }
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Failed to get current location: ${e.message}")
                        continuation.resumeWithException(e)
                    }

                    continuation.invokeOnCancellation {
                        cancellationTokenSource.cancel()
                    }
                }
            }
            Result.success(location)
        } catch (e: Exception) {
            Log.w(TAG, "Error getting current location (Timeout/Error): ${e.message}. Trying getLastLocation...")
            // Fallback: Last Known Location
            try {
                val lastLocation = fusedLocationClient.lastLocation.await()
                if (lastLocation != null) {
                    Log.d(TAG, "Last known location found: ${lastLocation.latitude}, ${lastLocation.longitude}")
                    Result.success(lastLocation)
                } else {
                    Result.failure(Exception("Failed to get both current and last known location"))
                }
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Failed to get last known location: ${fallbackEx.message}")
                Result.failure(e)
            }
        }
    }
}
