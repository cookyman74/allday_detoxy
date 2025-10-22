package com.allday.detoxy.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.allday.detoxy.core.manager.AutoRunAlarmManager
import com.allday.detoxy.receiver.AutoRunAlarmReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager 기반 자동 실행 Worker
 *
 * AlarmManager 실패 시 대체 수단으로 사용됩니다.
 * - Android 12+ 정확 알람 권한 없을 때
 * - 배터리 최적화로 인한 AlarmManager 실패 시
 *
 * ## 제약사항
 * - **정확도**: ±15분 (AlarmManager의 ±2분 대비 떨어짐)
 * - **Doze 모드**: WorkManager도 지연될 수 있음
 * - **사용자 경험**: 정확한 시간 실행이 중요한 경우 권한 요청 유도 필요
 *
 * ## WorkManager 설정
 * - `setInitialDelay()`: 다음 트리거 시각까지 지연
 * - `setConstraints()`: 배터리 절약 모드 무시 (가능한 범위 내)
 *
 * @param appContext Application Context
 * @param workerParams Worker 파라미터
 */
@HiltWorker
class AutoRunWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "AutoRunWorker"
        
        // Worker Input Data Keys (AutoRunAlarmManager와 동일)
        const val KEY_AUTO_RUN_ID = AutoRunAlarmManager.EXTRA_AUTO_RUN_ID
        const val KEY_DURATION_MINUTES = AutoRunAlarmManager.EXTRA_DURATION_MINUTES
        const val KEY_PRESET_TYPE = AutoRunAlarmManager.EXTRA_PRESET_TYPE
        const val KEY_LABEL = AutoRunAlarmManager.EXTRA_LABEL
        
        // Worker Unique Name Prefix
        const val WORK_NAME_PREFIX = "auto_run_"
    }

    /**
     * WorkManager 백그라운드 작업 실행
     *
     * AlarmManager의 BroadcastReceiver와 동일한 처리를 수행합니다.
     * AutoRunAlarmReceiver로 Intent를 전달하여 로직을 재사용합니다.
     *
     * @return Result.success() 또는 Result.retry()
     */
    override suspend fun doWork(): Result {
        Log.d(TAG, "🔄 AutoRunWorker started")

        // Input Data에서 autoRun 정보 추출
        val autoRunId = inputData.getString(KEY_AUTO_RUN_ID)
        val durationMinutes = inputData.getInt(KEY_DURATION_MINUTES, 0)
        val presetType = inputData.getString(KEY_PRESET_TYPE)
        val label = inputData.getString(KEY_LABEL)

        if (autoRunId == null) {
            Log.e(TAG, "❌ autoRunId is null, cannot process work")
            return Result.failure()
        }

        Log.i(
            TAG,
            "✅ AutoRun work received - ID: $autoRunId, Duration: $durationMinutes min, " +
                    "Preset: $presetType, Label: $label"
        )

        return try {
            // BroadcastReceiver와 동일한 처리를 위해 Intent 생성
            val intent = Intent(applicationContext, AutoRunAlarmReceiver::class.java).apply {
                action = AutoRunAlarmManager.ACTION_AUTO_RUN_ALARM
                putExtra(AutoRunAlarmManager.EXTRA_AUTO_RUN_ID, autoRunId)
                putExtra(AutoRunAlarmManager.EXTRA_DURATION_MINUTES, durationMinutes)
                putExtra(AutoRunAlarmManager.EXTRA_PRESET_TYPE, presetType)
                putExtra(AutoRunAlarmManager.EXTRA_LABEL, label)
            }

            // BroadcastReceiver 호출 (명시적 Intent)
            applicationContext.sendBroadcast(intent)
            
            Log.i(TAG, "✅ Broadcast sent to AutoRunAlarmReceiver")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to process AutoRun work: ${e.message}", e)
            Result.failure()
        }
    }
}

