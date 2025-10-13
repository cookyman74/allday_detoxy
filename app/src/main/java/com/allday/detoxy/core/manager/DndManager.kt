package com.allday.detoxy.core.manager

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * DND (Do Not Disturb) 모드 관리자 (디톡시 제어)
 *
 * 주요 기능:
 * 1. 타이머 실행 중 알림 차단으로 집중 모드 유지
 * 2. 권한 상태 명확한 반환 (DndPermissionState)
 * 3. 사용자 친화적 안내 문구 (디톡시 관점)
 * 4. 권한 설정 화면 Intent 제공
 *
 * Android 6.0 (API 23) 이상에서만 사용 가능
 * MVP: INTERRUPTION_FILTER_ALARMS (알람만 허용)
 */
class DndManager(private val context: Context) {

    companion object {
        private const val TAG = "DndManager"
    }

    /**
     * DND 권한 상태 (디톡시 제어 설정 화면에서 사용)
     */
    enum class DndPermissionState {
        /** 권한 부여됨 (DND 사용 가능) */
        GRANTED,

        /** 권한 거부됨 (설정 필요) */
        DENIED,

        /** API 지원 안 함 (Android 6.0 미만) */
        NOT_SUPPORTED;

        /**
         * 사용자 안내 문구 (디톡시 회복 관점)
         */
        fun getDescription(): String = when (this) {
            GRANTED -> "✅ 방해금지 모드가 활성화되어 집중력을 보호합니다"
            DENIED -> "⚠️ 방해금지 권한이 필요합니다. 알림으로부터 보호받지 못합니다"
            NOT_SUPPORTED -> "❌ 이 기기는 방해금지 모드를 지원하지 않습니다 (Android 6.0 이상 필요)"
        }

        /**
         * 액션 버튼 텍스트
         */
        fun getActionText(): String = when (this) {
            GRANTED -> "설정 변경"
            DENIED -> "권한 부여하기"
            NOT_SUPPORTED -> ""
        }

        /**
         * 권한이 필요한 상태인지
         */
        fun requiresPermission(): Boolean = this == DENIED
    }

    private val notificationManager: NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    // ==================== 권한 상태 확인 ====================

    /**
     * DND 권한 확인 (기존 boolean 반환 메서드, 하위 호환성)
     *
     * @return DND 설정 권한이 있으면 true, 아니면 false
     */
    fun hasNotificationPolicyAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager?.isNotificationPolicyAccessGranted ?: false
        } else {
            // Android 6.0 미만에서는 DND 기능 미지원
            false
        }
    }

    /**
     * DND 권한 상태 확인 (디톡시 제어 설정 화면용)
     *
     * UI에서 사용하기 좋은 enum 상태 반환
     *
     * @return DndPermissionState (GRANTED, DENIED, NOT_SUPPORTED)
     */
    fun getPermissionState(): DndPermissionState {
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> {
                DndPermissionState.NOT_SUPPORTED
            }
            hasNotificationPolicyAccess() -> {
                DndPermissionState.GRANTED
            }
            else -> {
                DndPermissionState.DENIED
            }
        }
    }

    /**
     * DND 권한 설정 화면 Intent 반환
     *
     * @return 권한 설정 화면으로 이동하는 Intent (지원하지 않으면 null)
     */
    fun getDndSettingsIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        } else {
            null
        }
    }

    /**
     * DND 모드 활성화
     *
     * 알람을 제외한 모든 알림을 차단합니다.
     * DND 권한이 없으면 실행되지 않습니다.
     *
     * @return 성공 여부
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun enableDnd(): Boolean {
        if (!hasNotificationPolicyAccess()) {
            Log.w(TAG, "DND permission not granted")
            return false
        }

        return try {
            // 알람만 허용하고 모든 알림 차단
            notificationManager?.setInterruptionFilter(
                NotificationManager.INTERRUPTION_FILTER_ALARMS
            )
            Log.d(TAG, "DND mode enabled (ALARMS only)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable DND: ${e.message}", e)
            false
        }
    }

    /**
     * DND 모드 비활성화
     *
     * 알림을 정상적으로 받을 수 있도록 복구합니다.
     *
     * @return 성공 여부
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun disableDnd(): Boolean {
        if (!hasNotificationPolicyAccess()) {
            Log.w(TAG, "DND permission not granted")
            return false
        }

        return try {
            // 모든 알림 허용
            notificationManager?.setInterruptionFilter(
                NotificationManager.INTERRUPTION_FILTER_ALL
            )
            Log.d(TAG, "DND mode disabled (ALL notifications)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable DND: ${e.message}", e)
            false
        }
    }

    /**
     * 현재 DND 상태 확인
     *
     * @return DND 활성화 여부
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun isDndEnabled(): Boolean {
        return try {
            val currentFilter = notificationManager?.currentInterruptionFilter
                ?: NotificationManager.INTERRUPTION_FILTER_ALL

            currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check DND status: ${e.message}", e)
            false
        }
    }

    /**
     * 현재 방해 금지 필터 모드 반환
     *
     * @return 현재 설정된 필터 모드
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun getCurrentInterruptionFilter(): Int {
        return notificationManager?.currentInterruptionFilter
            ?: NotificationManager.INTERRUPTION_FILTER_ALL
    }

    // ==================== 사용자 안내 (디톡시 관점) ====================

    /**
     * DND 모드 설명 (디톡시 회복 관점)
     *
     * @return 사용자에게 보여줄 DND 모드 설명
     */
    fun getDndDescription(): String {
        return when (val state = getPermissionState()) {
            DndPermissionState.GRANTED -> {
                "✅ 디톡시 세션 중 알림이 차단되어 집중력을 방해받지 않습니다.\n" +
                        "긴급 알람만 허용됩니다."
            }
            DndPermissionState.DENIED -> {
                "⚠️ 방해금지 권한이 필요합니다.\n" +
                        "알림으로부터 보호받지 못하면 디톡시 효과가 감소합니다."
            }
            DndPermissionState.NOT_SUPPORTED -> {
                "❌ 이 기기는 방해금지 모드를 지원하지 않습니다.\n" +
                        "(Android 6.0 이상 필요)"
            }
        }
    }

    /**
     * DND 상태 카드 정보 (디톡시 제어 설정 화면용)
     *
     * @return Triple<제목, 설명, 액션 텍스트>
     */
    fun getDndCardInfo(): Triple<String, String, String> {
        val state = getPermissionState()
        val title = when (state) {
            DndPermissionState.GRANTED -> "방해금지 모드 활성화됨"
            DndPermissionState.DENIED -> "방해금지 권한 필요"
            DndPermissionState.NOT_SUPPORTED -> "방해금지 모드 미지원"
        }
        return Triple(title, state.getDescription(), state.getActionText())
    }
}

