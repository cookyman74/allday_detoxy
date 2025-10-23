package com.allday.detoxy.core.utils

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 권한 관련 유틸리티 함수 모음
 *
 * 접근성 서비스, 오버레이 권한, DND 권한, 위치 권한 등의 상태 확인 및
 * 설정 화면 이동 기능을 제공합니다.
 */
object PermissionUtils {

    private const val TAG = "PermissionUtils"
    
    // 위치 권한 설명 텍스트
    val LOCATION_PERMISSION_EXPLANATION = """
        위치 기반 자동 실행을 사용하려면 위치 권한이 필요합니다.
        
        • 정확한 위치: 등록한 장소 도착 감지
        • 백그라운드 위치: 앱이 실행되지 않을 때도 자동 실행
        
        수집된 위치 정보는 기기에만 저장되며, 서버로 전송되지 않습니다.
    """.trimIndent()
    
    val BACKGROUND_LOCATION_RATIONALE = """
        백그라운드 위치 권한이 필요합니다.
        
        등록한 장소(회사, 학교 등)에 도착하면 자동으로 집중 모드를 시작하려면
        앱이 백그라운드에서 실행될 때도 위치 정보에 접근할 수 있어야 합니다.
        
        다음 화면에서 "항상 허용"을 선택해주세요.
    """.trimIndent()

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
    
    // ========================================
    // 위치 권한 관련 (Location Permissions)
    // ========================================
    
    /**
     * 정확한 위치 권한 확인 (ACCESS_FINE_LOCATION)
     *
     * 위치 기반 자동 실행을 위해서는 정확한 위치 권한이 필요합니다.
     *
     * @param context Android Context
     * @return 권한이 있으면 true, 아니면 false
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 대략적 위치 권한 확인 (ACCESS_COARSE_LOCATION)
     *
     * @param context Android Context
     * @return 권한이 있으면 true, 아니면 false
     */
    fun hasCoarseLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 백그라운드 위치 권한 확인 (ACCESS_BACKGROUND_LOCATION)
     *
     * Android 10 (API 29) 이상에서는 백그라운드 위치 권한을 별도로 요청해야 합니다.
     * 앱이 백그라운드에서 실행될 때도 위치 정보에 접근하려면 이 권한이 필요합니다.
     *
     * @param context Android Context
     * @return 권한이 있으면 true, Android 10 미만이면 true (별도 권한 불필요)
     */
    fun hasBackgroundLocationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 10 미만에서는 백그라운드 위치 권한이 별도로 필요하지 않음
            true
        }
    }
    
    /**
     * 위치 권한 완전 승인 여부 확인
     *
     * 정확한 위치 권한과 백그라운드 위치 권한이 모두 승인되었는지 확인합니다.
     *
     * @param context Android Context
     * @return 모든 필수 위치 권한이 승인되었으면 true
     */
    fun hasFullLocationPermission(context: Context): Boolean {
        return hasLocationPermission(context) && hasBackgroundLocationPermission(context)
    }
    
    /**
     * 위치 권한 상태를 문자열로 반환
     *
     * UI에서 현재 위치 권한 상태를 표시할 때 사용합니다.
     *
     * @param context Android Context
     * @return 권한 상태 문자열 ("모두 허용", "일부 허용", "거부됨")
     */
    fun getLocationPermissionStatus(context: Context): String {
        val hasFine = hasLocationPermission(context)
        val hasBackground = hasBackgroundLocationPermission(context)
        
        return when {
            hasFine && hasBackground -> "모두 허용" // 완전 승인
            hasFine -> "앱 사용 중에만 허용" // 포그라운드만
            else -> "거부됨" // 권한 없음
        }
    }
    
    /**
     * 위치 서비스 활성화 여부 확인
     *
     * 기기의 위치 서비스(GPS, 네트워크 위치)가 켜져 있는지 확인합니다.
     *
     * @param context Android Context
     * @return 위치 서비스가 활성화되어 있으면 true
     */
    fun isLocationServiceEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager?.isLocationEnabled ?: false
        } else {
            // Android P 미만에서는 GPS_PROVIDER 또는 NETWORK_PROVIDER 활성화 여부로 판단
            locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
            locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
        }
    }
    
    /**
     * 위치 설정 화면으로 이동
     *
     * 사용자가 위치 서비스를 켜거나 위치 권한을 변경할 수 있는 설정 화면으로 이동합니다.
     *
     * @param context Android Context
     */
    fun openLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open location settings", e)
        }
    }
    
    /**
     * 앱의 위치 권한 설정 화면으로 직접 이동
     *
     * 사용자가 앱 설정에서 위치 권한을 변경할 수 있도록 합니다.
     *
     * @param context Android Context
     */
    fun openAppLocationSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", context.packageName, null)
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app location settings", e)
        }
    }
    
    /**
     * 정확한 위치 권한 설명(Rationale)을 보여줘야 하는지 확인
     *
     * 사용자가 이전에 권한을 거부한 경우, 권한 요청 전에 추가 설명을 보여줘야 하는지 판단합니다.
     * 이 메서드가 true를 반환하면 권한 요청 다이얼로그를 띄우기 전에 사용자에게 
     * 왜 이 권한이 필요한지 설명해야 합니다.
     *
     * @param activity Activity 컨텍스트 (Fragment인 경우 requireActivity() 사용)
     * @return true: 설명을 보여줘야 함, false: 바로 권한 요청 가능
     */
    fun shouldShowLocationRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    
    /**
     * 대략적 위치 권한 설명(Rationale)을 보여줘야 하는지 확인
     *
     * @param activity Activity 컨텍스트
     * @return true: 설명을 보여줘야 함, false: 바로 권한 요청 가능
     */
    fun shouldShowCoarseLocationRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    /**
     * 백그라운드 위치 권한 설명(Rationale)을 보여줘야 하는지 확인
     *
     * Android 10 (API 29) 이상에서만 의미가 있습니다.
     * Android 10 미만에서는 항상 false를 반환합니다.
     *
     * @param activity Activity 컨텍스트
     * @return true: 설명을 보여줘야 함, false: 바로 권한 요청 가능 또는 Android 10 미만
     */
    fun shouldShowBackgroundLocationRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            // Android 10 미만에서는 백그라운드 위치 권한이 별도로 없음
            false
        }
    }
    
    // ==================== 알림 권한 (Android 13+) ====================
    
    /**
     * 알림 권한 부여 여부 확인
     *
     * Android 13 (API 33) 이상에서는 POST_NOTIFICATIONS 권한이 필요합니다.
     * Android 13 미만에서는 항상 true를 반환합니다 (알림 권한이 자동 부여됨).
     *
     * @param context Context
     * @return true: 알림 권한이 있음, false: 권한 없음 (Android 13+에서만 해당)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 13 미만에서는 알림 권한이 자동으로 부여됨
            true
        }
    }
    
    /**
     * 알림 권한 설명(Rationale)을 보여줘야 하는지 확인
     *
     * Android 13 (API 33) 이상에서만 의미가 있습니다.
     * 
     * @param activity Activity 컨텍스트
     * @return true: 설명을 보여줘야 함, false: 바로 권한 요청 가능 또는 Android 13 미만
     */
    fun shouldShowNotificationRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            false
        }
    }
    
    /**
     * 알림 설정 화면 열기
     *
     * 앱의 알림 설정 화면으로 이동합니다.
     * 사용자가 알림 권한을 영구적으로 거부한 경우 이 화면으로 안내합니다.
     *
     * @param context Context
     */
    fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android 8.0 이상: 앱 알림 설정 화면
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else {
                    // Android 8.0 미만: 앱 상세 설정 화면
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open notification settings", e)
        }
    }
}
