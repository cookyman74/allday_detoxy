package com.allday.detoxy.core.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * 배터리 최적화 관련 유틸리티
 * 
 * 각 제조사별 배터리 최적화 설정 화면 안내 및 상태 확인 기능을 제공합니다.
 * 
 * ## 지원 제조사
 * - Xiaomi (MIUI): 자동 시작, 배터리 세이버
 * - Samsung (One UI): 절전 앱 제외, 최적화되지 않음
 * - Huawei (EMUI): 앱 시작 관리, 보호된 앱
 * - OnePlus (OxygenOS): 배터리 최적화
 * - Oppo/Realme (ColorOS): 자동 시작, 배터리
 * - Vivo (FuntouchOS): 백그라운드 관리
 * 
 * @see <a href="https://dontkillmyapp.com">Don't Kill My App</a>
 */
object BatteryOptimizationUtils {
    
    private const val TAG = "BatteryOptimizationUtils"
    
    /**
     * 제조사 유형
     */
    enum class Manufacturer {
        XIAOMI,
        SAMSUNG,
        HUAWEI,
        ONEPLUS,
        OPPO,
        VIVO,
        REALME,
        LENOVO,
        ASUS,
        MEIZU,
        GOOGLE,  // Pixel, Android One
        OTHER
    }
    
    /**
     * 배터리 최적화 예외 상태
     */
    enum class BatteryOptimizationStatus {
        WHITELISTED,      // 예외 처리됨 (최적화 안함)
        NOT_WHITELISTED,  // 예외 처리 안됨 (최적화 적용 중)
        UNKNOWN           // 확인 불가
    }
    
    /**
     * 현재 기기의 제조사 확인
     */
    fun getManufacturer(): Manufacturer {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        
        return when {
            manufacturer.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") -> Manufacturer.XIAOMI
            manufacturer.contains("samsung") -> Manufacturer.SAMSUNG
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> Manufacturer.HUAWEI
            manufacturer.contains("oneplus") -> Manufacturer.ONEPLUS
            manufacturer.contains("oppo") -> Manufacturer.OPPO
            manufacturer.contains("vivo") -> Manufacturer.VIVO
            manufacturer.contains("realme") -> Manufacturer.REALME
            manufacturer.contains("lenovo") -> Manufacturer.LENOVO
            manufacturer.contains("asus") -> Manufacturer.ASUS
            manufacturer.contains("meizu") -> Manufacturer.MEIZU
            manufacturer.contains("google") -> Manufacturer.GOOGLE
            else -> Manufacturer.OTHER
        }
    }
    
    /**
     * 제조사가 적극적인 배터리 최적화를 적용하는지 확인
     * 
     * @return true: 추가 설정이 필요할 가능성이 높음
     */
    fun isAggressiveBatteryOptimizationManufacturer(): Boolean {
        return when (getManufacturer()) {
            Manufacturer.XIAOMI,
            Manufacturer.SAMSUNG,
            Manufacturer.HUAWEI,
            Manufacturer.ONEPLUS,
            Manufacturer.OPPO,
            Manufacturer.VIVO,
            Manufacturer.REALME,
            Manufacturer.MEIZU -> true
            Manufacturer.GOOGLE,
            Manufacturer.LENOVO,
            Manufacturer.ASUS,
            Manufacturer.OTHER -> false
        }
    }
    
    /**
     * Android 표준 배터리 최적화 예외 상태 확인
     * 
     * @param context Android Context
     * @return 배터리 최적화 예외 상태
     */
    fun getBatteryOptimizationStatus(context: Context): BatteryOptimizationStatus {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (pm.isIgnoringBatteryOptimizations(context.packageName)) {
                    BatteryOptimizationStatus.WHITELISTED
                } else {
                    BatteryOptimizationStatus.NOT_WHITELISTED
                }
            } else {
                // Android 6.0 미만은 배터리 최적화 기능 없음
                BatteryOptimizationStatus.WHITELISTED
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking battery optimization status: ${e.message}")
            BatteryOptimizationStatus.UNKNOWN
        }
    }
    
    /**
     * 배터리 최적화 예외 요청 다이얼로그 표시
     * 
     * ⚠️ Google Play 정책상 핵심 기능에 필수적인 경우에만 사용해야 합니다.
     * 
     * @param context Android Context
     * @return Intent를 성공적으로 시작했으면 true
     */
    fun requestBatteryOptimizationExemption(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Log.i(TAG, "✅ Battery optimization exemption request dialog shown")
                true
            } else {
                Log.d(TAG, "Battery optimization not applicable for API < 23")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to request battery optimization exemption: ${e.message}")
            false
        }
    }
    
    /**
     * 배터리 최적화 설정 화면으로 이동
     * 
     * @param context Android Context
     */
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open battery optimization settings: ${e.message}")
            // Fallback: 일반 배터리 설정
            openBatterySettings(context)
        }
    }
    
    /**
     * 배터리 설정 화면으로 이동
     * 
     * @param context Android Context
     */
    fun openBatterySettings(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open battery settings: ${e.message}")
            // Fallback: 앱 상세 설정
            openAppSettings(context)
        }
    }
    
    /**
     * 앱 상세 설정 화면으로 이동
     * 
     * @param context Android Context
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app settings: ${e.message}")
        }
    }
    
    // ========================================
    // 제조사별 자동 시작(AutoStart) 설정 화면 이동
    // ========================================
    
    /**
     * 제조사별 자동 시작 설정 화면으로 이동
     * 
     * @param context Android Context
     * @return Intent를 성공적으로 시작했으면 true
     */
    fun openAutoStartSettings(context: Context): Boolean {
        val manufacturer = getManufacturer()
        Log.d(TAG, "Opening auto-start settings for manufacturer: $manufacturer (${Build.MANUFACTURER})")
        
        val intents = getAutoStartIntents(context)
        
        for (intent in intents) {
            try {
                val resolveInfo = context.packageManager.resolveActivity(
                    intent, 
                    PackageManager.MATCH_DEFAULT_ONLY
                )
                if (resolveInfo != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Log.i(TAG, "✅ Opened auto-start settings: ${intent.component}")
                    return true
                }
            } catch (e: Exception) {
                Log.d(TAG, "Intent not available: ${intent.component}")
            }
        }
        
        // Fallback: 앱 설정 화면
        Log.w(TAG, "⚠️ No auto-start settings found, opening app settings")
        openAppSettings(context)
        return false
    }
    
    /**
     * 제조사별 자동 시작 설정 Intent 목록 생성
     */
    private fun getAutoStartIntents(context: Context): List<Intent> {
        val intents = mutableListOf<Intent>()
        
        when (getManufacturer()) {
            Manufacturer.XIAOMI -> {
                // MIUI 자동 시작 설정
                intents.add(Intent().apply {
                    component = android.content.ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                })
                // MIUI 보안 앱 (Fallback)
                intents.add(Intent().apply {
                    component = android.content.ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.MainActivity"
                    )
                })
            }
            Manufacturer.SAMSUNG -> {
                // Samsung 절전 설정
                intents.add(Intent().apply {
                    component = android.content.ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                })
                // Samsung Device Care
                intents.add(Intent().apply {
                    component = android.content.ComponentName(
                        "com.samsung.android.sm",
                        "com.samsung.android.sm.ui.battery.AppSleepListActivity"
                    )
                })
            }
            Manufacturer.HUAWEI -> {
                // Huawei 앱 시작 관리
                intents.add(Intent().apply {
                    component = android.content.ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                })
                intents.add(Intent().apply {
                    component = android.content.ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                })
            }
            Manufacturer.ONEPLUS -> {
                // OnePlus 배터리 최적화
                intents.add(Intent().apply {
                    component = android.content.ComponentName(
                        "com.oneplus.security",
                        "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                    )
                })
            }
            Manufacturer.OPPO -> {
                // Oppo 자동 시작
                intents.add(Intent().apply {
                    component = android.content.ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                })
                intents.add(Intent().apply {
                    component = android.content.ComponentName(
                        "com.oppo.safe",
                        "com.oppo.safe.permission.startup.StartupAppListActivity"
                    )
                })
            }
            Manufacturer.VIVO -> {
                // Vivo iManager
                intents.add(Intent().apply {
                    component = android.content.ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                })
                intents.add(Intent().apply {
                    component = android.content.ComponentName(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                    )
                })
            }
            Manufacturer.REALME -> {
                // Realme (ColorOS 기반)
                intents.add(Intent().apply {
                    component = android.content.ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                })
            }
            Manufacturer.LENOVO -> {
                // Lenovo
                intents.add(Intent().apply {
                    component = android.content.ComponentName(
                        "com.lenovo.security",
                        "com.lenovo.security.purebackground.PureBackgroundActivity"
                    )
                })
            }
            Manufacturer.ASUS -> {
                // Asus Mobile Manager
                intents.add(Intent().apply {
                    component = android.content.ComponentName(
                        "com.asus.mobilemanager",
                        "com.asus.mobilemanager.entry.FunctionActivity"
                    )
                })
            }
            Manufacturer.MEIZU -> {
                // Meizu
                intents.add(Intent().apply {
                    component = android.content.ComponentName(
                        "com.meizu.safe",
                        "com.meizu.safe.permission.SmartBGActivity"
                    )
                })
            }
            else -> {
                // 기타 제조사: 배터리 최적화 설정으로 이동
            }
        }
        
        return intents
    }
    
    // ========================================
    // 제조사별 가이드 텍스트
    // ========================================
    
    /**
     * 제조사별 배터리 최적화 해제 가이드 텍스트 반환
     */
    fun getBatteryOptimizationGuideText(): String {
        return when (getManufacturer()) {
            Manufacturer.XIAOMI -> """
                📱 Xiaomi (MIUI) 설정 가이드
                
                1️⃣ 자동 시작 허용
                • 보안 앱 → 권한 → 자동 시작
                • 'ScreenSence' 활성화
                
                2️⃣ 배터리 세이버 해제
                • 설정 → 앱 → ScreenSence → 배터리
                • '제한 없음' 선택
                
                3️⃣ 앱 잠금 (선택)
                • 최근 앱 화면에서 앱을 아래로 끌어 잠금
            """.trimIndent()
            
            Manufacturer.SAMSUNG -> """
                📱 Samsung (One UI) 설정 가이드
                
                1️⃣ 배터리 사용 무제한 설정
                • 설정 → 앱 → ScreenSence → 배터리
                • '무제한' 선택
                
                2️⃣ 절전 앱 제외
                • 설정 → 배터리 → 백그라운드 사용 제한
                • ScreenSence가 목록에 없는지 확인
                
                3️⃣ 배터리 최적화 제외
                • 설정 → 앱 → ⋮ → 특별 권한 → 배터리 사용량 최적화
                • 'ScreenSence' 최적화 해제
            """.trimIndent()
            
            Manufacturer.HUAWEI -> """
                📱 Huawei (EMUI) 설정 가이드
                
                1️⃣ 앱 시작 관리
                • 설정 → 앱 → 앱 시작
                • ScreenSence '수동 관리' 선택
                • 자동 시작, 백그라운드 실행 모두 활성화
                
                2️⃣ 보호된 앱 등록
                • 휴대폰 관리자 → 배터리 → 앱 잠금
                • ScreenSence 잠금 활성화
            """.trimIndent()
            
            Manufacturer.ONEPLUS -> """
                📱 OnePlus (OxygenOS) 설정 가이드
                
                1️⃣ 배터리 최적화 해제
                • 설정 → 배터리 → 배터리 최적화
                • ScreenSence '최적화하지 않음' 선택
                
                2️⃣ 최근 앱 잠금
                • 최근 앱 화면에서 ScreenSence 아이콘 누르기
                • '잠금' 선택
            """.trimIndent()
            
            Manufacturer.OPPO, Manufacturer.REALME -> """
                📱 Oppo/Realme (ColorOS) 설정 가이드
                
                1️⃣ 자동 시작 허용
                • 설정 → 앱 관리 → 앱 목록
                • ScreenSence → 자동 시작 활성화
                
                2️⃣ 배터리 최적화 해제
                • 설정 → 배터리 → 앱별 배터리
                • ScreenSence → '제한 없음' 선택
            """.trimIndent()
            
            Manufacturer.VIVO -> """
                📱 Vivo (FuntouchOS) 설정 가이드
                
                1️⃣ 백그라운드 실행 허용
                • iManager → 앱 관리 → 권한
                • ScreenSence → 백그라운드 실행 허용
                
                2️⃣ 화이트리스트 추가
                • iManager → 배터리 → 고전력 소비
                • ScreenSence 화이트리스트에 추가
            """.trimIndent()
            
            else -> """
                📱 배터리 최적화 해제 가이드
                
                앱이 백그라운드에서 정상적으로 작동하려면 
                배터리 최적화에서 제외해야 합니다.
                
                1️⃣ 설정 → 앱 → ScreenSence
                2️⃣ 배터리 → '제한 없음' 또는 '무제한' 선택
                3️⃣ 배터리 최적화 목록에서 제외
            """.trimIndent()
        }
    }
    
    /**
     * 제조사별 가이드 제목
     */
    fun getGuideTitle(): String {
        return when (getManufacturer()) {
            Manufacturer.XIAOMI -> "Xiaomi 배터리 설정"
            Manufacturer.SAMSUNG -> "Samsung 배터리 설정"
            Manufacturer.HUAWEI -> "Huawei 배터리 설정"
            Manufacturer.ONEPLUS -> "OnePlus 배터리 설정"
            Manufacturer.OPPO -> "Oppo 배터리 설정"
            Manufacturer.VIVO -> "Vivo 배터리 설정"
            Manufacturer.REALME -> "Realme 배터리 설정"
            else -> "배터리 설정"
        }
    }
    
    /**
     * 배터리 최적화가 필요한 상태인지 확인
     * 
     * 적극적인 배터리 최적화 제조사이면서 예외 처리가 안된 경우 true
     */
    fun needsBatteryOptimizationSetup(context: Context): Boolean {
        return isAggressiveBatteryOptimizationManufacturer() &&
               getBatteryOptimizationStatus(context) != BatteryOptimizationStatus.WHITELISTED
    }
}
