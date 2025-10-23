package com.allday.detoxy.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.allday.detoxy.core.manager.AutoRunAlarmManager
import com.allday.detoxy.core.manager.AutoRunNotificationManager
import com.allday.detoxy.data.local.dao.AutoRunLogDao
import com.allday.detoxy.data.local.entity.AutoRunLog
import com.allday.detoxy.worker.AutoStartTimerWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * 자동 실행 알림 액션 처리 BroadcastReceiver
 *
 * 자동 실행 알림의 액션 버튼 클릭을 처리합니다.
 *
 * ## 액션 종류
 * 1. **START**: 즉시 타이머 시작
 * 2. **SNOOZE**: 10분 후 다시 알림 (스누즈)
 * 3. **SKIP**: 이번 회차 건너뛰기
 *
 * ## 처리 흐름
 * 1. 액션 종류에 따라 분기
 * 2. AutoRunLog 기록
 * 3. 알림 해제
 * 4. 타이머 시작 또는 스누즈 처리
 *
 * @see AutoRunNotificationManager
 * @see AutoStartTimerWorker
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationActionReceiver"
        
        // Actions
        const val ACTION_START = "com.allday.detoxy.ACTION_AUTO_RUN_START"
        const val ACTION_SNOOZE = "com.allday.detoxy.ACTION_AUTO_RUN_SNOOZE"
        const val ACTION_SKIP = "com.allday.detoxy.ACTION_AUTO_RUN_SKIP"
        
        // Snooze delay
        private const val SNOOZE_DELAY_MINUTES = 10L
    }

    @Inject
    lateinit var notificationManager: AutoRunNotificationManager

    @Inject
    lateinit var alarmManager: AutoRunAlarmManager

    @Inject
    lateinit var autoRunLogDao: AutoRunLogDao

    @Inject
    lateinit var workManager: WorkManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val autoRunId = intent.getStringExtra(AutoRunNotificationManager.EXTRA_AUTO_RUN_ID) ?: return
        val durationMinutes = intent.getIntExtra(AutoRunNotificationManager.EXTRA_DURATION_MINUTES, 0)
        val presetType = intent.getStringExtra(AutoRunNotificationManager.EXTRA_PRESET_TYPE)
        val label = intent.getStringExtra(AutoRunNotificationManager.EXTRA_LABEL)
        val triggerType = intent.getStringExtra(AutoRunNotificationManager.EXTRA_TRIGGER_TYPE) ?: "TIME"

        Log.d(TAG, "Action received: ${intent.action} for autoRunId=$autoRunId")

        when (intent.action) {
            ACTION_START -> {
                handleStart(context, autoRunId, durationMinutes, presetType, label, triggerType)
            }
            ACTION_SNOOZE -> {
                handleSnooze(context, autoRunId, durationMinutes, presetType, label, triggerType)
            }
            ACTION_SKIP -> {
                handleSkip(context, autoRunId, triggerType)
            }
        }

        // 알림 해제
        notificationManager.dismissNotification(autoRunId, isPreNotification = false)
    }

    /**
     * 시작하기 액션 처리
     *
     * 즉시 타이머를 시작합니다.
     * WorkManager를 사용하여 타이머 시작 작업을 스케줄링합니다.
     *
     * @param context Context
     * @param autoRunId 자동 실행 ID
     * @param durationMinutes 타이머 시간 (분)
     * @param presetType 차단 프리셋 타입
     * @param label 자동 실행 라벨
     * @param triggerType 트리거 타입
     */
    private fun handleStart(
        context: Context,
        autoRunId: String,
        durationMinutes: Int,
        presetType: String?,
        label: String?,
        triggerType: String
    ) {
        Log.i(TAG, "🚀 Starting timer immediately: ${durationMinutes}분 (autoRunId=$autoRunId)")

        // AutoRunLog 기록
        scope.launch {
            try {
                val log = AutoRunLog(
                    triggerType = triggerType,
                    triggerSourceId = autoRunId,
                    triggerTime = System.currentTimeMillis(),
                    result = "STARTED",
                    failureReason = null,
                    sessionId = null // TODO: 타이머 시작 후 세션 ID 업데이트
                )
                autoRunLogDao.insert(log)
                Log.d(TAG, "✅ AutoRunLog recorded: STARTED")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to record AutoRunLog: ${e.message}", e)
            }
        }

        // 타이머 시작 (WorkManager 사용)
        val inputData = Data.Builder()
            .putString("autoRunId", autoRunId)
            .putInt("durationMinutes", durationMinutes)
            .putString("presetType", presetType)
            .putString("label", label)
            .build()

        val startTimerWork = OneTimeWorkRequest.Builder(AutoStartTimerWorker::class.java)
            .setInputData(inputData)
            .build()

        workManager.enqueue(startTimerWork)
        Log.d(TAG, "📋 AutoStartTimerWorker enqueued")
    }

    /**
     * 10분 후 액션 처리 (스누즈)
     *
     * 10분 후 다시 자동 실행 알림을 표시합니다.
     * WorkManager를 사용하여 지연 작업을 스케줄링합니다.
     *
     * @param context Context
     * @param autoRunId 자동 실행 ID
     * @param durationMinutes 타이머 시간 (분)
     * @param presetType 차단 프리셋 타입
     * @param label 자동 실행 라벨
     * @param triggerType 트리거 타입
     */
    private fun handleSnooze(
        context: Context,
        autoRunId: String,
        durationMinutes: Int,
        presetType: String?,
        label: String?,
        triggerType: String
    ) {
        Log.i(TAG, "⏰ Snoozing for ${SNOOZE_DELAY_MINUTES}분 (autoRunId=$autoRunId)")

        // AutoRunLog 기록
        scope.launch {
            try {
                val log = AutoRunLog(
                    triggerType = triggerType,
                    triggerSourceId = autoRunId,
                    triggerTime = System.currentTimeMillis(),
                    result = "SNOOZED",
                    failureReason = "User requested ${SNOOZE_DELAY_MINUTES}min snooze",
                    sessionId = null
                )
                autoRunLogDao.insert(log)
                Log.d(TAG, "✅ AutoRunLog recorded: SNOOZED")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to record AutoRunLog: ${e.message}", e)
            }
        }

        // 10분 후 다시 알림 표시 (WorkManager 사용)
        val inputData = Data.Builder()
            .putString("autoRunId", autoRunId)
            .putInt("durationMinutes", durationMinutes)
            .putString("presetType", presetType)
            .putString("label", label)
            .putString("triggerType", triggerType)
            .build()

        val snoozeWork = OneTimeWorkRequest.Builder(AutoStartTimerWorker::class.java)
            .setInputData(inputData)
            .setInitialDelay(SNOOZE_DELAY_MINUTES, TimeUnit.MINUTES)
            .addTag("snooze_$autoRunId")
            .build()

        workManager.enqueue(snoozeWork)
        Log.d(TAG, "📋 Snooze work enqueued (${SNOOZE_DELAY_MINUTES}분 후)")
    }

    /**
     * 건너뛰기 액션 처리
     *
     * 이번 회차 자동 실행을 건너뜁니다.
     * AutoRunLog에 SKIPPED 결과를 기록합니다.
     *
     * @param context Context
     * @param autoRunId 자동 실행 ID
     * @param triggerType 트리거 타입
     */
    private fun handleSkip(
        context: Context,
        autoRunId: String,
        triggerType: String
    ) {
        Log.i(TAG, "⏭️ Skipping auto-run (autoRunId=$autoRunId)")

        // AutoRunLog 기록
        scope.launch {
            try {
                val log = AutoRunLog(
                    triggerType = triggerType,
                    triggerSourceId = autoRunId,
                    triggerTime = System.currentTimeMillis(),
                    result = "SKIPPED",
                    failureReason = "User manually skipped",
                    sessionId = null
                )
                autoRunLogDao.insert(log)
                Log.d(TAG, "✅ AutoRunLog recorded: SKIPPED")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to record AutoRunLog: ${e.message}", e)
            }
        }
    }
}

