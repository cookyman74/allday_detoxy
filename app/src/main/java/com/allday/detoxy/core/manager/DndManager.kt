package com.allday.detoxy.core.manager

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * DND (Do Not Disturb) 모드 관리자
 *
 * 타이머 실행 중 알림을 차단하여 집중 모드를 유지합니다.
 * Android 6.0 (API 23) 이상에서만 사용 가능합니다.
 *
 * MVP 버전은 모든 알림을 차단하며, 알람만 허용합니다.
 * (INTERRUPTION_FILTER_ALARMS)
 */
class DndManager(private val context: Context) {

    companion object {
        private const val TAG = "DndManager"
    }

    private val notificationManager: NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    /**
     * DND 권한 확인
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

    /**
     * DND 모드 설명
     *
     * @return 사용자에게 보여줄 DND 모드 설명
     */
    fun getDndDescription(): String {
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> {
                "이 기기는 방해금지 모드를 지원하지 않습니다 (Android 6.0 이상 필요)"
            }
            !hasNotificationPolicyAccess() -> {
                "방해금지 모드를 사용하려면 권한이 필요합니다"
            }
            else -> {
                "타이머 실행 중 알람을 제외한 모든 알림이 차단됩니다"
            }
        }
    }
}
