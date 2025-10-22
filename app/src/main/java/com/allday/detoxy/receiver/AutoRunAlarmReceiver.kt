package com.allday.detoxy.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.allday.detoxy.core.manager.AutoRunAlarmManager

/**
 * 시간 기반 자동 실행 알람 BroadcastReceiver
 *
 * AlarmManager에서 등록한 알람이 트리거되면 호출됩니다.
 * 알림을 표시하고 사용자 응답을 기다립니다.
 *
 * ## 처리 흐름
 * 1. 알람 트리거 감지
 * 2. AutoRunLog 기록 준비
 * 3. 알림 표시 (AutoRunNotificationManager 호출)
 * 4. 다음 알람 자동 스케줄링
 *
 * TODO: AutoRunNotificationManager 구현 후 연동
 * TODO: AutoRunLog 기록 로직 추가
 * TODO: 이미 타이머 실행 중인지 확인 로직
 */
class AutoRunAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AutoRunAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔔 AutoRun alarm triggered - action: ${intent.action}")

        if (intent.action != AutoRunAlarmManager.ACTION_AUTO_RUN_ALARM) {
            Log.w(TAG, "⚠️ Unknown action: ${intent.action}")
            return
        }

        // Intent에서 autoRun 정보 추출
        val autoRunId = intent.getStringExtra(AutoRunAlarmManager.EXTRA_AUTO_RUN_ID)
        val durationMinutes = intent.getIntExtra(AutoRunAlarmManager.EXTRA_DURATION_MINUTES, 0)
        val presetType = intent.getStringExtra(AutoRunAlarmManager.EXTRA_PRESET_TYPE)
        val label = intent.getStringExtra(AutoRunAlarmManager.EXTRA_LABEL)

        if (autoRunId == null) {
            Log.e(TAG, "❌ autoRunId is null, cannot process alarm")
            return
        }

        Log.i(
            TAG,
            "✅ AutoRun alarm received - ID: $autoRunId, Duration: $durationMinutes min, " +
                    "Preset: $presetType, Label: $label"
        )

        // TODO: 이미 타이머 실행 중인지 확인
        // if (isTimerAlreadyRunning()) {
        //     Log.w(TAG, "⚠️ Timer already running, skipping auto-run")
        //     logAutoRunSkipped(autoRunId, "TIMER_ALREADY_RUNNING")
        //     return
        // }

        // TODO: AutoRunNotificationManager를 통해 알림 표시
        // AutoRunNotificationManager.showStartNotification(
        //     context = context,
        //     autoRunId = autoRunId,
        //     durationMinutes = durationMinutes,
        //     label = label ?: "자동 실행"
        // )
        
        Log.i(TAG, "📢 Notification should be shown here (TODO: implement AutoRunNotificationManager)")

        // TODO: AutoRunLog 기록
        // logAutoRunTriggered(
        //     autoRunId = autoRunId,
        //     triggerType = "TIME",
        //     result = "NOTIFICATION_SHOWN"
        // )

        // TODO: 다음 알람 자동 스케줄링
        // - DB에서 autoRun 정보 다시 가져오기
        // - calculateNextTriggerTime() 호출
        // - scheduleTimeBasedAutoRun() 호출
        
        Log.i(TAG, "⏰ Next alarm should be scheduled here (TODO: implement reschedule logic)")
    }
}

