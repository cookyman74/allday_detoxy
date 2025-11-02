package com.allday.detoxy.core.manager

import android.content.Context
import android.location.Location
import android.util.Log
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 위치 충돌 해소 관리자 (Phase 3)
 *
 * 여러 위치 반경이 겹칠 때 우선순위 규칙에 따라 단일 위치를 선택합니다.
 *
 * ## 우선순위 규칙 (기반: 스케줄가동프로세스.md):
 * 1. **면적(반경) 우선**: 반경이 가장 작은 위치들만 남김
 * 2. **히스테리시스 (120초)**: 직전 활성 위치가 후보에 포함되고, 120초 이내면 유지
 *    - 새 위치 전환 시 타이머 리셋
 *    - 만료 시 즉시 재평가
 * 3. **거리 우선**: 사용자 위치에서 가장 가까운 위치
 * 4. **최근 수정(updatedAt) 우선**: 가장 최근에 수정된 위치
 *
 * ## 히스테리시스 동작:
 * - 위치 전환 발생 시: 새 위치로 타이머 리셋 (`recordActivation()` 호출)
 * - 히스테리시스 만료 시: 자동으로 재평가 (거리 우선으로 전환)
 * - 직전 활성 위치가 후보에서 제외 시: 즉시 재평가
 *
 * @param context ApplicationContext
 */
@Singleton
class LocationConflictResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ConflictResolver"
        
        /**
         * 히스테리시스 시간 (초)
         * 
         * 직전 활성 위치가 후보에 포함되고, 이 시간 이내면 기존 위치를 유지합니다.
         * 타이머는 새 위치로 전환 시 리셋되며, 만료 시 즉시 재평가됩니다.
         */
        private const val HYSTERESIS_SECONDS = 120  // 2분
    }
    
    /**
     * 직전 활성 위치 ID (히스테리시스용)
     */
    private var lastActiveLocationId: String? = null
    
    /**
     * 직전 활성화 시각 (밀리초)
     */
    private var lastActivationTime: Long = 0
    
    /**
     * 위치 충돌 해소
     *
     * 여러 위치 후보 중 우선순위 규칙에 따라 단일 위치를 선택합니다.
     *
     * @param currentLocation 사용자의 현재 위치
     * @param candidates 반경 내 포함되는 위치 후보들
     * @return 선택된 위치 (후보가 없으면 null)
     */
    suspend fun resolveConflict(
        currentLocation: Location,
        candidates: List<LocationBasedAutoRun>
    ): LocationBasedAutoRun? {
        
        // 후보가 없으면 null 반환
        if (candidates.isEmpty()) {
            Log.d(TAG, "No candidates")
            return null
        }
        
        // 후보가 1개면 즉시 반환
        if (candidates.size == 1) {
            Log.d(TAG, "Single candidate: ${candidates.first().label}")
            return candidates.first()
        }
        
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔍 Conflict Resolution Start")
        Log.d(TAG, "Candidates: ${candidates.size}")
        candidates.forEach { 
            Log.d(TAG, "  - ${it.label} (radius: ${it.radiusMeters}m)")
        }
        
        // 1. 면적(반경) 우선: 반경이 가장 작은 위치들만 남김
        val minRadius = candidates.minOf { it.radiusMeters }
        var filtered = candidates.filter { it.radiusMeters == minRadius }
        
        Log.d(TAG, "📐 Step 1: Area Priority (min radius: ${minRadius}m)")
        Log.d(TAG, "  Filtered: ${filtered.size} candidates")
        
        if (filtered.size == 1) {
            Log.d(TAG, "✅ Winner: ${filtered.first().label} (area)")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            return filtered.first()
        }
        
        // 2. 히스테리시스: 직전 활성 위치가 후보에 포함되고, 120초 이내면 유지
        val lastActive = lastActiveLocationId
        val timeSinceActivation = (System.currentTimeMillis() - lastActivationTime) / 1000
        
        Log.d(TAG, "⏱️ Step 2: Hysteresis")
        Log.d(TAG, "  Last active: $lastActive")
        Log.d(TAG, "  Time since activation: ${timeSinceActivation}s (threshold: ${HYSTERESIS_SECONDS}s)")
        
        if (lastActive != null && timeSinceActivation <= HYSTERESIS_SECONDS) {
            val lastActiveInCandidates = filtered.find { it.id == lastActive }
            if (lastActiveInCandidates != null) {
                Log.d(TAG, "✅ Winner: ${lastActiveInCandidates.label} (hysteresis)")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                return lastActiveInCandidates
            } else {
                Log.d(TAG, "  ⚠️ Last active not in candidates → reset hysteresis")
                resetHysteresis()
            }
        } else if (timeSinceActivation > HYSTERESIS_SECONDS) {
            Log.d(TAG, "  ⚠️ Hysteresis expired → reset and re-evaluate")
            resetHysteresis()
        } else {
            Log.d(TAG, "  No last active location")
        }
        
        // 3. 거리 우선: 사용자 위치에서 가장 가까운 위치
        val distances = filtered.map { location ->
            val targetLocation = Location("").apply {
                latitude = location.latitude
                longitude = location.longitude
            }
            val distance = currentLocation.distanceTo(targetLocation)
            location to distance
        }
        
        Log.d(TAG, "📏 Step 3: Distance Priority")
        distances.forEach { (loc, dist) ->
            Log.d(TAG, "  - ${loc.label}: ${String.format("%.1f", dist)}m")
        }
        
        val minDistance = distances.minOf { it.second }
        filtered = distances.filter { it.second == minDistance }.map { it.first }
        
        Log.d(TAG, "  Min distance: ${String.format("%.1f", minDistance)}m")
        Log.d(TAG, "  Filtered: ${filtered.size} candidates")
        
        if (filtered.size == 1) {
            Log.d(TAG, "✅ Winner: ${filtered.first().label} (distance)")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            return filtered.first()
        }
        
        // 4. 최근 수정(updatedAt) 우선
        val winner = filtered.maxByOrNull { it.updatedAt }  // Phase 0에서 추가된 필드
        
        Log.d(TAG, "🕐 Step 4: Recent Modification Priority")
        filtered.forEach { loc ->
            Log.d(TAG, "  - ${loc.label}: updatedAt = ${loc.updatedAt}")
        }
        Log.d(TAG, "✅ Winner: ${winner?.label} (recent modification)")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        return winner
    }
    
    /**
     * 활성화 기록 (히스테리시스용)
     *
     * 새 위치로 전환 시 호출하여 타이머를 리셋합니다.
     * 동일한 위치를 재활성화하는 경우에도 타이머를 리셋합니다.
     *
     * @param locationId 활성화된 위치 ID
     */
    fun recordActivation(locationId: String) {
        val previousLocation = lastActiveLocationId
        lastActiveLocationId = locationId
        lastActivationTime = System.currentTimeMillis()
        
        if (previousLocation != locationId) {
            Log.d(TAG, "📝 Location switched: $previousLocation → $locationId (timer reset)")
        } else {
            Log.d(TAG, "📝 Same location re-activated: $locationId (timer reset)")
        }
    }
    
    /**
     * 히스테리시스 상태 초기화
     *
     * 다음 상황에서 호출됩니다:
     * - 직전 활성 위치가 후보에서 제외됨
     * - 히스테리시스 시간 만료 (120초 초과)
     */
    fun resetHysteresis() {
        lastActiveLocationId = null
        lastActivationTime = 0
        Log.d(TAG, "🔄 Hysteresis reset")
    }
    
    /**
     * 현재 히스테리시스 상태 조회 (디버깅용)
     *
     * @return 직전 활성 위치 ID (없으면 null)
     */
    fun getLastActiveLocationId(): String? = lastActiveLocationId
    
    /**
     * 히스테리시스 남은 시간 조회 (초 단위, 디버깅용)
     *
     * @return 남은 시간 (초), 만료 시 0
     */
    fun getRemainingHysteresisSeconds(): Long {
        if (lastActiveLocationId == null) return 0
        
        val elapsed = (System.currentTimeMillis() - lastActivationTime) / 1000
        val remaining = HYSTERESIS_SECONDS - elapsed
        
        return if (remaining > 0) remaining else 0
    }
}

