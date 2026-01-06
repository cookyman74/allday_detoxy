package com.allday.detoxy.core.manager

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.service.notification.ZenDeviceEffects
import android.service.notification.ZenPolicy
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.allday.detoxy.presentation.ui.settings.GrayscaleSettingsActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 흑백 모드 관리자 (Android 15+ AutomaticZenRule 기반)
 * 
 * 집중 모드 활성화 시 화면을 그레이스케일로 전환하여 스마트폰 사용 욕구 감소
 * 
 * 주요 기능:
 * 1. AutomaticZenRule + ZenDeviceEffects를 통한 자동 흑백 전환
 * 2. 앱 단일 룰 전략 (전체 앱에서 1개의 룰만 사용)
 * 3. 룰 ID 영속화 (앱 재시작 시 복원)
 * 4. 시스템 룰 삭제 감지 및 상태 동기화
 * 
 * @see DndManager 패턴 참조
 * @see ZenDeviceEffects.setShouldDisplayGrayscale
 */
@Singleton
class GrayscaleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GrayscaleManager"
        private const val PREFS_NAME = "grayscale_prefs"
        private val KEY_RULE_ID = stringPreferencesKey("grayscale_zen_rule_id")
        private const val RULE_NAME = "AllDay Detoxy 집중 모드"
    }

    // DataStore 인스턴스
    private val Context.dataStore by preferencesDataStore(name = PREFS_NAME)

    // ⚠️ 비동기 처리용 CoroutineScope (runBlocking 대체 - ANR 방지)
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 현재 활성화 상태
    private var isGrayscaleActive = false
    
    // 현재 룰 ID (메모리 캐시)
    private var ruleId: String? = null

    // ==================== 공개 메서드 ====================

    /**
     * 그레이스케일 활성화 (조건 확인 후)
     * 
     * - 이미 활성화된 상태면 스킵
     * - Android 15 미만이면 false 반환
     * - 권한 없으면 false 반환
     * 
     * @return 활성화 성공 여부
     */
    fun enableGrayscaleIfNeeded(): Boolean {
        Log.d(TAG, "enableGrayscaleIfNeeded() called, isGrayscaleActive=$isGrayscaleActive")
        
        // 이미 활성화 상태면 스킵
        if (isGrayscaleActive) {
            Log.d(TAG, "Already active, skipping")
            return true
        }
        
        // API 레벨 체크
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            Log.w(TAG, "Android 15+ required for grayscale mode")
            return false
        }
        
        return enableGrayscaleInternal()
    }

    /**
     * 그레이스케일 비활성화 (조건 확인 후)
     * 
     * - 이미 비활성화 상태면 스킵
     * 
     * @return 비활성화 성공 여부
     */
    fun disableGrayscaleIfNeeded(): Boolean {
        Log.d(TAG, "disableGrayscaleIfNeeded() called, isGrayscaleActive=$isGrayscaleActive")
        
        // 이미 비활성화 상태면 스킵
        if (!isGrayscaleActive) {
            Log.d(TAG, "Already inactive, skipping")
            return true
        }
        
        // API 레벨 체크
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // Android 14 이하는 그레이스케일 기능 자체가 없으므로 성공 처리
            return true
        }
        
        return disableGrayscaleInternal()
    }

    /**
     * 룰 존재 여부 검증
     * 
     * 시스템에서 사용자가 룰을 삭제한 경우 상태 동기화
     * 
     * @return 룰이 존재하면 true
     */
    fun validateRuleExists(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return false
        }
        
        return validateRuleExistsInternal()
    }

    /**
     * 앱 시작 시 룰 ID 복원 (비동기)
     * 
     * ⚠️ 리뷰 반영: runBlocking 제거, 비동기 처리로 ANR 방지
     */
    fun restoreRuleId() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return
        }
        
        managerScope.launch {
            try {
                ruleId = context.dataStore.data.first()[KEY_RULE_ID]
                Log.d(TAG, "Restored ruleId: $ruleId")
                
                // 룰이 존재하는지 확인하고 상태 동기화 (메인 스레드에서 실행)
                if (ruleId != null) {
                    withContext(Dispatchers.Main) {
                        validateRuleExists()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore ruleId: ${e.message}", e)
            }
        }
    }

    /**
     * 현재 활성화 상태 확인
     */
    fun isActive(): Boolean = isGrayscaleActive

    /**
     * 그레이스케일 지원 여부 확인
     */
    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

    /**
     * 권한 상태 확인
     */
    fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        return nm.isNotificationPolicyAccessGranted
    }

    /**
     * 그레이스케일 적용 가능 여부 확인
     * 
     * 다음 조건을 모두 만족해야 true:
     * 1. Android 15+ (API 35)
     * 2. ACCESS_NOTIFICATION_POLICY 권한 부여됨
     * 
     * ⚠️ 설정 ON 상태는 별도로 확인 필요 (Repository에서 관리)
     * 
     * @return 그레이스케일 적용 가능 여부
     */
    fun shouldApplyGrayscale(): Boolean {
        return isSupported() && hasPermission()
    }

    // ==================== 내부 메서드 (Android 15+) ====================

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun enableGrayscaleInternal(): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java)
        
        // 권한 확인
        if (!nm.isNotificationPolicyAccessGranted) {
            Log.w(TAG, "Notification policy access not granted")
            return false
        }
        
        // 사용자 관리 모드 확인
        if (nm.areAutomaticZenRulesUserManaged()) {
            Log.w(TAG, "Automatic zen rules are user managed")
            return false
        }
        
        return try {
            // ZenDeviceEffects 생성 - 그레이스케일만 적용
            val effects = ZenDeviceEffects.Builder()
                .setShouldDisplayGrayscale(true)
                .build()
            
            // ZenPolicy - 알림 차단 없이 효과만 적용
            val policy = ZenPolicy.Builder()
                .allowAllSounds()
                .build()
            
            // ⚠️ 리뷰 반영: ComponentName 클래스 참조 사용 (문자열 대신)
            // AutomaticZenRule 생성
            val rule = AutomaticZenRule.Builder(RULE_NAME, Uri.EMPTY)
                .setType(AutomaticZenRule.TYPE_SCHEDULE_TIME)  // Android 15+ API
                .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                .setZenPolicy(policy)
                .setDeviceEffects(effects)
                .setConfigurationActivity(
                    ComponentName(context, GrayscaleSettingsActivity::class.java)
                )
                .setEnabled(true)
                .build()
            
            // ⚠️ 리뷰 반영: 룰 추가/업데이트 후 null 체크 추가
            val resultRuleId: String? = if (ruleId != null) {
                try {
                    nm.updateAutomaticZenRule(ruleId!!, rule)
                    Log.d(TAG, "Updated existing rule: $ruleId")
                    ruleId
                } catch (e: Exception) {
                    // 기존 룰이 삭제됐을 수 있음 → 새로 생성 시도
                    Log.w(TAG, "Failed to update rule, creating new: ${e.message}")
                    nm.addAutomaticZenRule(rule)
                }
            } else {
                nm.addAutomaticZenRule(rule)
            }
            
            // ⚠️ 리뷰 반영: 룰 생성 실패 (null) 시 false 반환
            if (resultRuleId == null) {
                Log.e(TAG, "❌ Failed to create/update rule: returned null (TYPE_SCHEDULE_TIME + Uri.EMPTY may not be supported)")
                isGrayscaleActive = false
                return false
            }
            
            ruleId = resultRuleId
            Log.d(TAG, "Created new rule: $resultRuleId")
            
            // 룰 ID 저장 (비동기)
            saveRuleIdAsync(ruleId)
            
            isGrayscaleActive = true
            Log.i(TAG, "✅ Grayscale mode enabled")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to enable grayscale: ${e.message}", e)
            isGrayscaleActive = false  // 실패 시 명시적으로 false 설정
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun disableGrayscaleInternal(): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java)
        
        return try {
            ruleId?.let { id ->
                // 룰 비활성화 (삭제하지 않고 비활성화)
                val existingRules = nm.automaticZenRules
                existingRules[id]?.let { existingRule ->
                    val disabledRule = AutomaticZenRule.Builder(existingRule)
                        .setEnabled(false)
                        .build()
                    nm.updateAutomaticZenRule(id, disabledRule)
                    Log.d(TAG, "Disabled rule: $id")
                }
            }
            
            isGrayscaleActive = false
            Log.i(TAG, "✅ Grayscale mode disabled")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to disable grayscale: ${e.message}", e)
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun validateRuleExistsInternal(): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java)
        
        return try {
            ruleId?.let { id ->
                val rules = nm.automaticZenRules
                if (!rules.containsKey(id)) {
                    // 시스템에서 삭제됨 → 상태 정리
                    Log.w(TAG, "Rule $id was deleted externally")
                    isGrayscaleActive = false
                    clearRuleIdAsync()
                    return false
                }
                
                // 룰 활성화 상태 동기화
                val rule = rules[id]
                isGrayscaleActive = rule?.isEnabled == true
                Log.d(TAG, "Rule exists, isEnabled=${rule?.isEnabled}")
                return isGrayscaleActive
            }
            
            // ruleId가 null이면 활성화된 룰 없음
            isGrayscaleActive = false
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate rule: ${e.message}", e)
            false
        }
    }

    // ==================== 저장소 메서드 (비동기) ====================

    /**
     * 룰 ID 저장 (비동기)
     * 
     * ⚠️ 리뷰 반영: runBlocking 제거, 비동기 처리로 ANR 방지
     */
    private fun saveRuleIdAsync(id: String?) {
        managerScope.launch {
            try {
                context.dataStore.edit { preferences ->
                    if (id != null) {
                        preferences[KEY_RULE_ID] = id
                    } else {
                        preferences.remove(KEY_RULE_ID)
                    }
                }
                Log.d(TAG, "Saved ruleId: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save ruleId: ${e.message}", e)
            }
        }
    }

    /**
     * 룰 ID 삭제 (비동기)
     */
    private fun clearRuleIdAsync() {
        ruleId = null
        saveRuleIdAsync(null)
        Log.d(TAG, "Cleared ruleId")
    }
}

