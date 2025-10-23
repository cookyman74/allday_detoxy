package com.allday.detoxy.core.manager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.allday.detoxy.R
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.receiver.NotificationActionReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 자동 실행 알림 관리 클래스
 *
 * 시간 기반 및 위치 기반 자동 실행 알림을 생성하고 관리합니다.
 *
 * ## 알림 종류
 * 1. **사전 알림** (Pre-notification): 실제 실행 N분 전에 표시
 * 2. **실행 알림** (Start notification): 자동 실행 시각에 표시 (액션 버튼 포함)
 *
 * ## 알림 액션
 * - **시작하기**: 즉시 타이머 시작
 * - **10분 후**: 10분 스누즈
 * - **건너뛰기**: 이번 회차 건너뛰기
 *
 * @param context Application Context
 * @param notificationManager NotificationManager
 */
@Singleton
class AutoRunNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager
) {

    companion object {
        private const val TAG = "AutoRunNotificationManager"
        
        // Notification Channel
        const val CHANNEL_ID = "auto_run_notifications"
        private const val CHANNEL_NAME = "자동 실행 알림"
        private const val CHANNEL_DESCRIPTION = "시간/위치 기반 자동 실행 알림"
        
        // Notification IDs
        private const val NOTIFICATION_ID_PREFIX = 10000
        
        // Intent Extras
        const val EXTRA_AUTO_RUN_ID = "autoRunId"
        const val EXTRA_DURATION_MINUTES = "durationMinutes"
        const val EXTRA_PRESET_TYPE = "presetType"
        const val EXTRA_LABEL = "label"
        const val EXTRA_TRIGGER_TYPE = "triggerType"
    }

    init {
        createNotificationChannel()
    }

    /**
     * 알림 채널 생성 (Android 8.0+)
     *
     * 중요도: HIGH (소리, 진동, 상단 표시)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 사전 알림 표시
     *
     * 실제 자동 실행 N분 전에 표시하는 알림입니다.
     * 액션 버튼 없이 정보 제공만 합니다.
     *
     * @param autoRunId 자동 실행 ID
     * @param durationMinutes 타이머 시간 (분)
     * @param label 자동 실행 라벨
     * @param minutesBefore 몇 분 전 알림인지
     * @param triggerType 트리거 타입 (TIME 또는 LOCATION)
     */
    fun showPreNotification(
        autoRunId: String,
        durationMinutes: Int,
        label: String?,
        minutesBefore: Int,
        triggerType: String = "TIME"
    ) {
        val notificationId = generateNotificationId(autoRunId, isPreNotification = true)
        
        val title = "곧 자동 실행 예정"
        val content = buildString {
            append("${minutesBefore}분 후 ")
            if (label != null) {
                append("\"$label\" ")
            }
            append("자동 실행이 시작됩니다 (${durationMinutes}분)")
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: 적절한 아이콘으로 변경
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(notificationId, notification)
    }

    /**
     * 실행 알림 표시
     *
     * 자동 실행 시각에 표시하는 알림입니다.
     * 3개의 액션 버튼 (시작하기, 10분 후, 건너뛰기)을 포함합니다.
     *
     * @param autoRunId 자동 실행 ID
     * @param durationMinutes 타이머 시간 (분)
     * @param presetType 차단 프리셋 타입
     * @param label 자동 실행 라벨
     * @param triggerType 트리거 타입 (TIME 또는 LOCATION)
     */
    fun showStartNotification(
        autoRunId: String,
        durationMinutes: Int,
        presetType: String?,
        label: String?,
        triggerType: String = "TIME"
    ) {
        val notificationId = generateNotificationId(autoRunId, isPreNotification = false)
        
        val title = if (label != null) {
            "\"$label\" 자동 실행"
        } else {
            "자동 실행 시작"
        }
        
        val content = buildString {
            append("${durationMinutes}분 타이머")
            if (presetType != null) {
                append(" (${getPresetTypeDisplayName(presetType)})")
            }
            append(" · 시작하시겠습니까?")
        }
        
        // 액션 PendingIntent 생성
        val startIntent = createActionPendingIntent(
            autoRunId, durationMinutes, presetType, label, triggerType,
            NotificationActionReceiver.ACTION_START
        )
        
        val snoozeIntent = createActionPendingIntent(
            autoRunId, durationMinutes, presetType, label, triggerType,
            NotificationActionReceiver.ACTION_SNOOZE
        )
        
        val skipIntent = createActionPendingIntent(
            autoRunId, durationMinutes, presetType, label, triggerType,
            NotificationActionReceiver.ACTION_SKIP
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: 적절한 아이콘으로 변경
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false) // 액션을 눌러야 사라지도록
            .setOngoing(true) // 스와이프로 삭제 방지
            .addAction(
                R.drawable.ic_launcher_foreground, // TODO: 적절한 아이콘으로 변경
                "시작하기",
                startIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground, // TODO: 적절한 아이콘으로 변경
                "10분 후",
                snoozeIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground, // TODO: 적절한 아이콘으로 변경
                "건너뛰기",
                skipIntent
            )
            .build()
        
        notificationManager.notify(notificationId, notification)
    }

    /**
     * 알림 해제
     *
     * @param autoRunId 자동 실행 ID
     * @param isPreNotification 사전 알림 여부 (기본: false, 실행 알림)
     */
    fun dismissNotification(autoRunId: String, isPreNotification: Boolean = false) {
        val notificationId = generateNotificationId(autoRunId, isPreNotification)
        notificationManager.cancel(notificationId)
    }

    /**
     * 액션 PendingIntent 생성
     *
     * @param autoRunId 자동 실행 ID
     * @param durationMinutes 타이머 시간 (분)
     * @param presetType 차단 프리셋 타입
     * @param label 자동 실행 라벨
     * @param triggerType 트리거 타입
     * @param action 액션 (START, SNOOZE, SKIP)
     * @return PendingIntent
     */
    private fun createActionPendingIntent(
        autoRunId: String,
        durationMinutes: Int,
        presetType: String?,
        label: String?,
        triggerType: String,
        action: String
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_AUTO_RUN_ID, autoRunId)
            putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
            putExtra(EXTRA_PRESET_TYPE, presetType)
            putExtra(EXTRA_LABEL, label)
            putExtra(EXTRA_TRIGGER_TYPE, triggerType)
        }
        
        // 각 액션마다 고유한 requestCode 사용
        val requestCode = kotlin.math.abs(autoRunId.hashCode()) + action.hashCode()
        
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Notification ID 생성
     *
     * autoRunId를 기반으로 고유한 Notification ID를 생성합니다.
     * 사전 알림과 실행 알림은 다른 ID를 가집니다.
     *
     * @param autoRunId 자동 실행 ID
     * @param isPreNotification 사전 알림 여부
     * @return Notification ID
     */
    private fun generateNotificationId(autoRunId: String, isPreNotification: Boolean): Int {
        val baseId = kotlin.math.abs(autoRunId.hashCode()) % 10000
        return if (isPreNotification) {
            NOTIFICATION_ID_PREFIX + baseId
        } else {
            NOTIFICATION_ID_PREFIX + 5000 + baseId
        }
    }

    /**
     * 프리셋 타입 표시 이름 반환
     *
     * @param presetType 프리셋 타입 (FULL_BLOCK, STANDARD, RELAXED)
     * @return 표시 이름
     */
    private fun getPresetTypeDisplayName(presetType: String): String {
        return when (presetType) {
            "FULL_BLOCK" -> "완전 차단"
            "STANDARD" -> "표준"
            "RELAXED" -> "완화"
            else -> "표준"
        }
    }
}

