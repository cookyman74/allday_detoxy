package com.allday.detoxy.core.utils

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log

/**
 * 권한 관련 유틸리티 함수 모음
 *
 * 접근성 서비스, 오버레이 권한, DND 권한 등의 상태 확인 및
 * 설정 화면 이동 기능을 제공합니다.
 */
object PermissionUtils {

    private const val TAG = "PermissionUtils"

    /**
     * 접근성 서비스 활성화 여부 확인
     *
     * @param context Android Context
     * @return 접근성 서비스가 활성화되어 있으면 true, 아니면 false
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val service = "${context.packageName}/com.allday.detoxy.service.accessibility.FocusAccessibilityService"

        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )

            Log.d(TAG, "Checking accessibility service: $service")
            Log.d(TAG, "Enabled services: $enabledServices")

            if (enabledServices.isNullOrEmpty()) {
                Log.w(TAG, "No accessibility services enabled")
                false
            } else {
                val colonSplitter = TextUtils.SimpleStringSplitter(':')
                colonSplitter.setString(enabledServices)

                while (colonSplitter.hasNext()) {
                    val componentName = colonSplitter.next()
                    if (componentName.equals(service, ignoreCase = true)) {
                        Log.i(TAG, "✅ Accessibility service is ENABLED")
                        return true
                    }
                }
                Log.w(TAG, "❌ Accessibility service is NOT enabled")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility service", e)
            false
        }
    }

    /**
     * 접근성 설정 화면으로 이동
     *
     * @param context Android Context
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * 다른 앱 위에 표시 권한 확인
     *
     * @param context Android Context
     * @return 권한이 있으면 true, 아니면 false
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * 오버레이 권한 설정 화면으로 이동
     *
     * @param context Android Context
     */
    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * DND (방해금지 모드) 권한 확인
     *
     * @param context Android Context
     * @return DND 권한이 있으면 true, 아니면 false (Android 6.0 미만은 false)
     */
    fun hasNotificationPolicyAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.isNotificationPolicyAccessGranted ?: false
        } else {
            false
        }
    }

    /**
     * DND 권한 설정 화면으로 이동
     *
     * @param context Android Context
     */
    fun openNotificationPolicySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
