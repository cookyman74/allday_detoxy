package com.allday.detoxy.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.allday.detoxy.core.manager.AutoRunNotificationManager
import com.allday.detoxy.service.timer.FocusTimerService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 자동 시작 타이머 Worker
 *
 * 자동 실행 알림의 "시작하기" 버튼 클릭 또는 자동 시작 딜레이 후 타이머를 시작합니다.
 *
 * ## 처리 흐름
 * 1. InputData에서 autoRun 정보 추출
 * 2. FocusTimerService 시작 Intent 생성
 * 3. Service 시작
 *
 * ## 사용 시나리오
 * 1. **즉시 시작**: NotificationActionReceiver.ACTION_START
 * 2. **스누즈 후 알림**: 10분 후 다시 알림 표시
 * 3. **자동 시작 딜레이**: N분 후 자동으로 타이머 시작
 *
 * @param context Context
 * @param workerParams WorkerParameters
 */
@HiltWorker
class AutoStartTimerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationManager: AutoRunNotificationManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "AutoStartTimerWorker"
        const val WORK_NAME_PREFIX = "auto_start_timer_"
    }

    override suspend fun doWork(): Result {
        try {
            val autoRunId = inputData.getString("autoRunId") ?: return Result.failure()
            val durationMinutes = inputData.getInt("durationMinutes", 0)
            val presetType = inputData.getString("presetType")
            val label = inputData.getString("label")
            val triggerType = inputData.getString("triggerType") ?: "TIME"
            val isSnooze = inputData.getBoolean("isSnooze", false)

            Log.i(TAG, "🚀 Starting timer: ${durationMinutes}분 (autoRunId=$autoRunId, isSnooze=$isSnooze)")

            if (isSnooze) {
                // 스누즈 후 다시 알림 표시
                notificationManager.showStartNotification(
                    autoRunId = autoRunId,
                    durationMinutes = durationMinutes,
                    presetType = presetType,
                    label = label,
                    triggerType = triggerType
                )
                Log.d(TAG, "📢 Snooze notification shown again")
            } else {
                // 타이머 시작
                val intent = Intent(applicationContext, FocusTimerService::class.java).apply {
                    action = FocusTimerService.ACTION_START
                    putExtra(FocusTimerService.EXTRA_DURATION_MINUTES, durationMinutes)
                    putExtra(FocusTimerService.EXTRA_PRESET_TYPE, presetType ?: "STANDARD")
                    putExtra(FocusTimerService.EXTRA_AUTO_RUN_ID, autoRunId)
                    putExtra(FocusTimerService.EXTRA_AUTO_RUN_LABEL, label)
                }

                applicationContext.startForegroundService(intent)
                Log.d(TAG, "✅ FocusTimerService started")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start timer: ${e.message}", e)
            return Result.failure()
        }
    }
}

