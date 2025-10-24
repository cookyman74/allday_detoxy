package com.allday.detoxy.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.allday.detoxy.core.manager.AutoRunAlarmManager
import com.allday.detoxy.core.manager.AutoRunNotificationManager
import com.allday.detoxy.data.local.dao.AutoRunLogDao
import com.allday.detoxy.data.local.dao.TimeBasedAutoRunDao
import com.allday.detoxy.data.local.entity.AutoRunLog
import com.allday.detoxy.domain.repository.AutoRunSettingsRepository
import com.allday.detoxy.service.timer.FocusTimerService
import com.allday.detoxy.worker.AutoStartTimerWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 시간 기반 자동 실행 알람 BroadcastReceiver
 *
 * AlarmManager에서 등록한 알람이 트리거되면 호출됩니다.
 * 알림을 표시하고 사용자 응답을 기다립니다.
 *
 * ## 처리 흐름
 * 1. 알람 트리거 감지 (ACTION_AUTO_RUN_ALARM 또는 ACTION_PRE_NOTIFICATION)
 * 2. 사전 알림 or 실행 알림 처리
 * 3. 다음 알람 자동 스케줄링 (실행 알림일 때만)
 * 4. AutoRunLog 기록
 *
 * ## Hilt 이슈 대응
 * - ⚠️ @AndroidEntryPoint 제거: Hilt ASM 변환 오류로 인해 수동 의존성 주입 사용
 * - EntryPointAccessors를 통해 수동으로 의존성 가져오기
 * 
 * @see com.allday.detoxy.domain.repository.AutoRunSettingsRepository
 * @see docs/02_advanced_autosetting_todolist.md §3.3
 */
class AutoRunAlarmReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AutoRunAlarmReceiverEntryPoint {
        fun alarmManager(): AutoRunAlarmManager
        fun timeBasedAutoRunDao(): TimeBasedAutoRunDao
        fun notificationManager(): AutoRunNotificationManager
        fun autoRunLogDao(): AutoRunLogDao
        fun autoRunSettingsRepository(): AutoRunSettingsRepository
        fun workManager(): WorkManager
    }

    companion object {
        private const val TAG = "AutoRunAlarmReceiver"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔔 AutoRun alarm triggered - action: ${intent.action}")

        // Intent에서 autoRun 정보 추출
        val autoRunId = intent.getStringExtra(AutoRunAlarmManager.EXTRA_AUTO_RUN_ID)
        val durationMinutes = intent.getIntExtra(AutoRunAlarmManager.EXTRA_DURATION_MINUTES, 0)
        val presetType = intent.getStringExtra(AutoRunAlarmManager.EXTRA_PRESET_TYPE)
        val label = intent.getStringExtra(AutoRunAlarmManager.EXTRA_LABEL)
        val isPreNotification = intent.getBooleanExtra(AutoRunAlarmManager.EXTRA_IS_PRE_NOTIFICATION, false)

        if (autoRunId == null) {
            Log.e(TAG, "❌ autoRunId is null, cannot process alarm")
            return
        }

        // ⚠️ Hilt 이슈 대응: EntryPointAccessors를 통해 수동으로 의존성 가져오기
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            AutoRunAlarmReceiverEntryPoint::class.java
        )

        when (intent.action) {
            AutoRunAlarmManager.ACTION_PRE_NOTIFICATION -> {
                // 사전 알림
                Log.i(TAG, "📢 Pre-notification triggered for: ${label ?: autoRunId}")
                handlePreNotification(context, autoRunId, durationMinutes, label, entryPoint)
            }
            AutoRunAlarmManager.ACTION_AUTO_RUN_ALARM -> {
                // 실행 알림
                Log.i(TAG, "✅ Auto-run alarm triggered - ID: $autoRunId, Duration: $durationMinutes min, Preset: $presetType, Label: $label")
                handleAutoRunAlarm(context, autoRunId, durationMinutes, presetType, label, entryPoint)
            }
            else -> {
                Log.w(TAG, "⚠️ Unknown action: ${intent.action}")
            }
        }
    }

    /**
     * 사전 알림 처리
     *
     * 실제 자동 실행 N분 전에 사용자에게 알림을 표시합니다.
     */
    private fun handlePreNotification(
        context: Context,
        autoRunId: String,
        durationMinutes: Int,
        label: String?,
        entryPoint: AutoRunAlarmReceiverEntryPoint
    ) {
        Log.i(TAG, "📢 Pre-notification: ${label ?: autoRunId} (${durationMinutes}분 타이머 예정)")
        
        scope.launch {
            try {
                // 사전 알림 시간 가져오기
                val preNotificationMinutes = entryPoint.autoRunSettingsRepository().getPreNotificationMinutes()
                
                // 사전 알림 표시
                entryPoint.notificationManager().showPreNotification(
                    autoRunId = autoRunId,
                    durationMinutes = durationMinutes,
                    label = label,
                    minutesBefore = preNotificationMinutes,
                    triggerType = "TIME"
                )
                
                Log.d(TAG, "✅ Pre-notification shown")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to show pre-notification: ${e.message}", e)
            }
        }
    }

    /**
     * 자동 실행 알람 처리
     *
     * 알림을 표시하고 다음 알람을 자동으로 스케줄링합니다.
     *
     * ## 구현 상태 (3.3) ✅
     * - ✅ 다음 알람 자동 스케줄링 (주간 반복)
     * - ✅ 알림 표시 (AutoRunNotificationManager)
     * - ✅ 자동 시작 딜레이 (autoStartDelayMinutes 적용) ⚠️ **Critical**
     * - ✅ AutoRunLog 기록
     *
     * ## 자동 시작 딜레이 로직 (⚠️ Critical - UI와 동작 일치)
     * - autoStartDelayMinutes == 0: 알림만 표시, 사용자 액션 대기
     * - autoStartDelayMinutes > 0: 알림 표시 + N분 후 자동 시작 (WorkManager)
     * 
     * @see com.allday.detoxy.domain.repository.AutoRunSettingsRepository.getAutoStartDelayMinutes
     * @see docs/02_advanced_autosetting_todolist.md §3.3.1, §3.3.2
     */
    private fun handleAutoRunAlarm(
        context: Context,
        autoRunId: String,
        durationMinutes: Int,
        presetType: String?,
        label: String?,
        entryPoint: AutoRunAlarmReceiverEntryPoint
    ) {
        // goAsync()를 사용하여 비동기 작업 완료 보장
        val pendingResult = goAsync()

        scope.launch {
            try {
                // 1. 이미 타이머 실행 중인지 확인
                val currentState = FocusTimerService.state.first()
                if (currentState != com.allday.detoxy.domain.model.FocusState.IDLE) {
                    Log.w(TAG, "⚠️ Timer already running (state: $currentState), skipping auto-run")
                    logAutoRunSkipped(autoRunId, "TIMER_ALREADY_RUNNING", entryPoint)
                    return@launch
                }

                // 2. 실행 알림 표시
                entryPoint.notificationManager().showStartNotification(
                    autoRunId = autoRunId,
                    durationMinutes = durationMinutes,
                    presetType = presetType,
                    label = label,
                    triggerType = "TIME"
                )
                Log.d(TAG, "✅ Start notification shown")

                // 3. AutoRunLog 기록 (알림 표시됨)
                logAutoRunTriggered(autoRunId, "TIME", "NOTIFICATION_SHOWN", entryPoint)

                // 4. 자동 시작 딜레이 적용 ✅ Fixed
                val autoStartDelayMinutes = entryPoint.autoRunSettingsRepository().getAutoStartDelayMinutes()
                
                if (autoStartDelayMinutes > 0) {
                    // N분 후 자동 시작 스케줄링 (WorkManager)
                    scheduleAutoStart(autoRunId, durationMinutes, presetType, label, autoStartDelayMinutes, entryPoint)
                    Log.i(TAG, "⏰ Auto-start scheduled: ${autoStartDelayMinutes}분 후 자동 시작")
                } else {
                    // 0분이면 즉시 타이머 시작 (알림 표시 후 바로 실행)
                    Log.i(TAG, "🚀 Auto-start delay is 0, starting timer immediately")
                    
                    // 고유 세션 ID 생성
                    val sessionId = java.util.UUID.randomUUID().toString()
                    
                    // FocusTimerService 즉시 시작
                    val startIntent = Intent(context, com.allday.detoxy.service.timer.FocusTimerService::class.java).apply {
                        action = com.allday.detoxy.service.timer.FocusTimerService.ACTION_START
                        putExtra(com.allday.detoxy.service.timer.FocusTimerService.EXTRA_DURATION_MINUTES, durationMinutes)
                        putExtra(com.allday.detoxy.service.timer.FocusTimerService.EXTRA_SESSION_ID, sessionId)
                        putExtra(com.allday.detoxy.service.timer.FocusTimerService.EXTRA_PRESET_TYPE, presetType)
                        putExtra(com.allday.detoxy.service.timer.FocusTimerService.EXTRA_AUTO_RUN_ID, autoRunId)
                        putExtra(com.allday.detoxy.service.timer.FocusTimerService.EXTRA_AUTO_RUN_LABEL, label)
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(startIntent)
                    } else {
                        context.startService(startIntent)
                    }
                    
                    // 알림 즉시 해제 (타이머가 시작되면 알림 불필요)
                    entryPoint.notificationManager().dismissNotification(autoRunId, isPreNotification = false)
                    
                    // AutoRunLog 기록 (자동 시작됨)
                    scope.launch {
                        logAutoRunStarted(autoRunId, "TIME", sessionId, entryPoint)
                    }
                    
                    Log.d(TAG, "✅ Timer started immediately (auto-start delay = 0, sessionId: $sessionId)")
                }

                // 5. 다음 알람 자동 스케줄링
                rescheduleNextAlarm(autoRunId, entryPoint)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error processing auto-run alarm: ${e.message}", e)
                logAutoRunFailed(autoRunId, "TIME", e.message ?: "Unknown error", entryPoint)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * 자동 시작 스케줄링 (WorkManager)
     *
     * autoStartDelayMinutes 설정에 따라 N분 후 타이머를 자동으로 시작합니다.
     * 사용자가 알림에 반응하지 않을 때 자동으로 타이머를 시작하는 기능입니다.
     *
     * @param autoRunId 자동 실행 ID
     * @param durationMinutes 타이머 시간 (분)
     * @param presetType 차단 프리셋 타입
     * @param label 자동 실행 라벨
     * @param delayMinutes 지연 시간 (분)
     */
    private fun scheduleAutoStart(
        autoRunId: String,
        durationMinutes: Int,
        presetType: String?,
        label: String?,
        delayMinutes: Int,
        entryPoint: AutoRunAlarmReceiverEntryPoint
    ) {
        val inputData = Data.Builder()
            .putString("autoRunId", autoRunId)
            .putInt("durationMinutes", durationMinutes)
            .putString("presetType", presetType)
            .putString("label", label)
            .putBoolean("isSnooze", false)
            .build()

        val autoStartWork = OneTimeWorkRequest.Builder(AutoStartTimerWorker::class.java)
            .setInputData(inputData)
            .setInitialDelay(delayMinutes.toLong(), TimeUnit.MINUTES)
            .addTag("auto_start_$autoRunId")
            .build()

        entryPoint.workManager().enqueue(autoStartWork)
        Log.d(TAG, "📋 Auto-start work enqueued (${delayMinutes}분 후)")
    }

    /**
     * AutoRunLog 기록 - 트리거됨
     */
    private suspend fun logAutoRunTriggered(autoRunId: String, triggerType: String, result: String, entryPoint: AutoRunAlarmReceiverEntryPoint) {
        try {
            val log = AutoRunLog(
                triggerType = triggerType,
                triggerSourceId = autoRunId,
                triggerTime = System.currentTimeMillis(),
                result = result,
                failureReason = null,
                sessionId = null
            )
            entryPoint.autoRunLogDao().insert(log)
            Log.d(TAG, "✅ AutoRunLog recorded: $result")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to record AutoRunLog: ${e.message}", e)
        }
    }

    /**
     * AutoRunLog 기록 - 건너뜀
     */
    private suspend fun logAutoRunSkipped(autoRunId: String, reason: String, entryPoint: AutoRunAlarmReceiverEntryPoint) {
        try {
            val log = AutoRunLog(
                triggerType = "TIME",
                triggerSourceId = autoRunId,
                triggerTime = System.currentTimeMillis(),
                result = "SKIPPED",
                failureReason = reason,
                sessionId = null
            )
            entryPoint.autoRunLogDao().insert(log)
            Log.d(TAG, "✅ AutoRunLog recorded: SKIPPED ($reason)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to record AutoRunLog: ${e.message}", e)
        }
    }

    /**
     * AutoRunLog 기록 - 실패
     */
    private suspend fun logAutoRunFailed(autoRunId: String, triggerType: String, reason: String, entryPoint: AutoRunAlarmReceiverEntryPoint) {
        try {
            val log = AutoRunLog(
                triggerType = triggerType,
                triggerSourceId = autoRunId,
                triggerTime = System.currentTimeMillis(),
                result = "FAILED",
                failureReason = reason,
                sessionId = null
            )
            entryPoint.autoRunLogDao().insert(log)
            Log.d(TAG, "✅ AutoRunLog recorded: FAILED ($reason)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to record AutoRunLog: ${e.message}", e)
        }
    }

    /**
     * AutoRunLog 기록 - 자동 시작됨
     * 
     * 자동 시작 딜레이가 0분이거나 WorkManager에 의해 자동으로 타이머가 시작된 경우 기록합니다.
     * 
     * @param autoRunId 자동 실행 ID
     * @param triggerType 트리거 타입 (TIME/LOCATION)
     * @param sessionId 시작된 세션 ID
     */
    private suspend fun logAutoRunStarted(autoRunId: String, triggerType: String, sessionId: String, entryPoint: AutoRunAlarmReceiverEntryPoint) {
        try {
            val log = AutoRunLog(
                triggerType = triggerType,
                triggerSourceId = autoRunId,
                triggerTime = System.currentTimeMillis(),
                result = "STARTED",
                failureReason = null,
                sessionId = sessionId
            )
            entryPoint.autoRunLogDao().insert(log)
            Log.d(TAG, "✅ AutoRunLog recorded: STARTED (sessionId=$sessionId)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to record AutoRunLog: ${e.message}", e)
        }
    }

    /**
     * 다음 알람 자동 스케줄링
     *
     * DB에서 최신 autoRun 정보를 가져와 다음 발생 시각을 계산하여 알람을 재등록합니다.
     * 주간 반복 알람을 지속적으로 유지하기 위해 필수적입니다.
     *
     * ## 🔧 Critical Fix: Flow 무한 구독 문제 해결
     * - 기존: `collect { ... }` 사용 → Flow가 완료되지 않아 코루틴이 무한히 살아있음
     * - 수정: `firstOrNull()` 사용 → 단일 스냅샷만 가져와 즉시 완료
     * - 영향: `pendingResult.finish()`에 정상 도달, goAsync() 윈도우 시간 초과 방지
     *
     * @param autoRunId 재스케줄링할 autoRun ID
     */
    private suspend fun rescheduleNextAlarm(autoRunId: String, entryPoint: AutoRunAlarmReceiverEntryPoint) {
        try {
            // 🔧 Critical Fix: firstOrNull() 사용하여 단일 스냅샷만 가져오기
            // collect()를 사용하면 Flow가 완료되지 않아 코루틴이 무한히 살아있게 됨
            val autoRun = entryPoint.timeBasedAutoRunDao().getById(autoRunId).firstOrNull()
            
            if (autoRun == null) {
                Log.w(TAG, "⚠️ AutoRun not found in DB: $autoRunId, cannot reschedule")
                return
            }

            if (!autoRun.isEnabled) {
                Log.d(TAG, "⏭️ AutoRun is disabled: $autoRunId, skipping reschedule")
                return
            }

            // 다음 알람 스케줄링
            val success = entryPoint.alarmManager().scheduleTimeBasedAutoRun(autoRun)
            
            if (success) {
                Log.i(TAG, "✅ Next alarm scheduled for: ${autoRun.label ?: autoRunId}")
            } else {
                Log.e(TAG, "❌ Failed to schedule next alarm for: ${autoRun.label ?: autoRunId}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error rescheduling alarm: ${e.message}", e)
        }
    }
}

