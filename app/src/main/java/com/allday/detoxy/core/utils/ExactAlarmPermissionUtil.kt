package com.allday.detoxy.core.utils

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

/**
 * 정확 알람 권한 관련 유틸리티
 *
 * Android 12+ (API 31)부터 도입된 SCHEDULE_EXACT_ALARM 권한을 관리합니다.
 * 시간 기반 자동 실행에 필수적인 권한입니다.
 *
 * ## 사용 예시
 * ```kotlin
 * if (!ExactAlarmPermissionUtil.canScheduleExactAlarms(context)) {
 *     // 권한 요청 다이얼로그 표시
 *     val intent = ExactAlarmPermissionUtil.createSettingsIntent(context)
 *     startActivity(intent)
 * }
 * ```
 *
 * ## 권한 설명
 * - **필요 이유**: 정확한 시간(±2분)에 자동 실행 트리거
 * - **없을 경우**: WorkManager fallback (±15분 오차)
 * - **사용자 경험**: 시간 기반 자동 실행의 정확도 저하
 */
object ExactAlarmPermissionUtil {

    /**
     * 정확 알람 권한 가능 여부 확인
     *
     * Android 12 미만: 항상 true
     * Android 12+: SCHEDULE_EXACT_ALARM 권한 확인
     *
     * @param context Context
     * @return true: 정확 알람 사용 가능, false: WorkManager fallback 필요
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Android 11 이하는 권한 불필요
        }
    }

    /**
     * 정확 알람 설정 화면으로 이동하는 Intent 생성
     *
     * Android 12+ (API 31) 이상에서만 사용 가능합니다.
     *
     * ## 설정 화면
     * - 경로: 설정 > 앱 > 특수 액세스 > 정확한 알람
     * - 사용자가 직접 권한 허용/거부 토글 가능
     *
     * @param context Context
     * @return 설정 화면 Intent
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun createSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * 정확 알람 설정 화면으로 안전하게 이동 (API 레벨 체크 포함)
     *
     * Android 12 이상: 정확 알람 설정 화면
     * Android 11 이하: 앱 정보 화면 (대체)
     *
     * @param context Context
     * @return 설정 화면 Intent (null이 아님을 보장)
     */
    fun createSettingsIntentSafe(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            createSettingsIntent(context)
        } else {
            // Android 11 이하: 앱 정보 화면으로 대체
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    /**
     * 현재 스케줄러 타입 반환
     *
     * UI에서 사용자에게 표시할 스케줄러 정보를 제공합니다.
     *
     * @param context Context
     * @return "정확 알람 (±2분)" 또는 "근사 알람 (±15분)"
     */
    fun getSchedulerType(context: Context): String {
        return if (canScheduleExactAlarms(context)) {
            "정확 알람 (±2분)"
        } else {
            "근사 알람 (±15분)"
        }
    }

    /**
     * 권한 요청이 필요한지 여부
     *
     * Android 12+ 에서만 true를 반환합니다.
     *
     * @return true: 권한 요청 UI 표시 필요, false: 권한 요청 불필요
     */
    fun shouldRequestPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    /**
     * 권한 설명 텍스트
     *
     * 사용자에게 왜 이 권한이 필요한지 설명합니다.
     */
    val PERMISSION_EXPLANATION = """
        정확한 시간에 집중 모드를 시작하려면 '정확한 알람' 권한이 필요합니다.
        
        • 권한 허용 시: 설정한 시간에 정확히 시작 (±2분)
        • 권한 거부 시: 근사 시간에 시작 (±15분 오차 발생)
        
        배터리에 영향을 주지 않으며, 앱의 핵심 기능입니다.
    """.trimIndent()

    /**
     * 권한 경고 배너 텍스트 (짧은 버전)
     */
    const val PERMISSION_WARNING_SHORT = "정확한 시간 실행을 위해 권한을 설정해주세요"

    /**
     * WorkManager fallback 안내 텍스트
     */
    const val FALLBACK_NOTICE = "현재 근사 알람 모드로 동작 중입니다 (±15분 오차 발생 가능)"
}

