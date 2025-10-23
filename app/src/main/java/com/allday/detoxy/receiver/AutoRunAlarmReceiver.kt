package com.allday.detoxy.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.allday.detoxy.core.manager.AutoRunAlarmManager
import com.allday.detoxy.data.local.dao.TimeBasedAutoRunDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

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
 * 4. AutoRunLog 기록 (TODO: 3.3)
 *
 * ## 다음 알람 스케줄링 ✅
 * 실행 알람 트리거 시 DB에서 autoRun을 조회하여 다음 알람을 자동으로 스케줄링합니다.
 * 이를 통해 주간 반복 알람을 지속적으로 유지할 수 있습니다.
 *
 * ## TODO (3.3.1, 3.3.2) - 알림 및 자동 시작
 * - AutoRunNotificationManager 구현 후 알림 표시 연동
 * - AutoRunLog 기록 로직 추가
 * - 이미 타이머 실행 중인지 확인 로직
 * - 자동 시작 딜레이 (autoStartDelayMinutes) 적용
 * 
 * @see com.allday.detoxy.domain.repository.AutoRunSettingsRepository
 * @see docs/02_advanced_autosetting_todolist.md §3.3.1, §3.3.2
 */
@AndroidEntryPoint
class AutoRunAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AutoRunAlarmReceiver"
    }

    @Inject
    lateinit var alarmManager: AutoRunAlarmManager

    @Inject
    lateinit var timeBasedAutoRunDao: TimeBasedAutoRunDao

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

        when (intent.action) {
            AutoRunAlarmManager.ACTION_PRE_NOTIFICATION -> {
                // 사전 알림
                Log.i(TAG, "📢 Pre-notification triggered for: ${label ?: autoRunId}")
                handlePreNotification(context, autoRunId, durationMinutes, label)
            }
            AutoRunAlarmManager.ACTION_AUTO_RUN_ALARM -> {
                // 실행 알림
                Log.i(TAG, "✅ Auto-run alarm triggered - ID: $autoRunId, Duration: $durationMinutes min, Preset: $presetType, Label: $label")
                handleAutoRunAlarm(context, autoRunId, durationMinutes, presetType, label)
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
     *
     * TODO (3.3.1): AutoRunNotificationManager.showPreNotification() 연동
     */
    private fun handlePreNotification(context: Context, autoRunId: String, durationMinutes: Int, label: String?) {
        Log.i(TAG, "📢 Pre-notification: ${label ?: autoRunId} (${durationMinutes}분 타이머 예정)")
        
        // TODO (3.3.1): AutoRunNotificationManager를 통해 사전 알림 표시
        // AutoRunNotificationManager.showPreNotification(
        //     context = context,
        //     autoRunId = autoRunId,
        //     durationMinutes = durationMinutes,
        //     label = label ?: "자동 실행"
        // )
        
        Log.i(TAG, "📢 Pre-notification should be shown here (TODO: implement AutoRunNotificationManager)")
    }

    /**
     * 자동 실행 알람 처리
     *
     * 알림을 표시하고 다음 알람을 자동으로 스케줄링합니다.
     *
     * ## 현재 구현 상태 (3.2)
     * - ✅ 다음 알람 자동 스케줄링 (주간 반복)
     * - ❌ 알림 표시 (3.3.1에서 구현 예정)
     * - ❌ 자동 시작 딜레이 (3.3.2에서 구현 예정)
     * - ❌ AutoRunLog 기록 (3.3.2에서 구현 예정)
     *
     * ## ⚠️ 중요: UI와 동작 불일치 (2차 리뷰 지적)
     * UI에서 "자동 시작 딜레이" 옵션을 노출하고 있지만, 실제 동작은 3.3.2에서 구현 예정입니다.
     * 현재는 알람 트리거 시 로그만 출력하며, 사용자가 설정한 딜레이는 적용되지 않습니다.
     * 
     * **권장 조치** (3.3 작업 전):
     * - UI에서 "자동 시작 딜레이" 옵션을 임시로 숨기거나
     * - "다음 업데이트에서 적용 예정" 안내 표시
     *
     * ## TODO (3.3.1, 3.3.2) - 다음 작업에서 구현
     * - [ ] AutoRunNotificationManager.showStartNotification() 연동
     * - [ ] 자동 시작 딜레이 (autoStartDelayMinutes) 적용 ⚠️ **Critical**
     * - [ ] AutoRunLog 기록
     * - [ ] 이미 타이머 실행 중인지 확인
     * 
     * @see com.allday.detoxy.domain.repository.AutoRunSettingsRepository.getAutoStartDelayMinutes
     * @see docs/02_advanced_autosetting_todolist.md §3.3.1, §3.3.2
     */
    private fun handleAutoRunAlarm(context: Context, autoRunId: String, durationMinutes: Int, presetType: String?, label: String?) {
        // goAsync()를 사용하여 비동기 작업 완료 보장
        val pendingResult = goAsync()

        scope.launch {
            try {
                // TODO (3.3.2): 이미 타이머 실행 중인지 확인
                // if (isTimerAlreadyRunning()) {
                //     Log.w(TAG, "⚠️ Timer already running, skipping auto-run")
                //     logAutoRunSkipped(autoRunId, "TIMER_ALREADY_RUNNING")
                //     return@launch
                // }

                // TODO (3.3.2): AutoRunNotificationManager를 통해 알림 표시
                // AutoRunNotificationManager.showStartNotification(
                //     context = context,
                //     autoRunId = autoRunId,
                //     durationMinutes = durationMinutes,
                //     label = label ?: "자동 실행"
                // )
                
                Log.i(TAG, "📢 Start notification should be shown here (TODO: implement AutoRunNotificationManager)")

                // TODO (3.3.2): AutoRunLog 기록
                // logAutoRunTriggered(
                //     autoRunId = autoRunId,
                //     triggerType = "TIME",
                //     result = "NOTIFICATION_SHOWN"
                // )

                // ✅ 다음 알람 자동 스케줄링
                rescheduleNextAlarm(autoRunId)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error processing auto-run alarm: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
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
    private suspend fun rescheduleNextAlarm(autoRunId: String) {
        try {
            // 🔧 Critical Fix: firstOrNull() 사용하여 단일 스냅샷만 가져오기
            // collect()를 사용하면 Flow가 완료되지 않아 코루틴이 무한히 살아있게 됨
            val autoRun = timeBasedAutoRunDao.getById(autoRunId).firstOrNull()
            
            if (autoRun == null) {
                Log.w(TAG, "⚠️ AutoRun not found in DB: $autoRunId, cannot reschedule")
                return
            }

            if (!autoRun.isEnabled) {
                Log.d(TAG, "⏭️ AutoRun is disabled: $autoRunId, skipping reschedule")
                return
            }

            // 다음 알람 스케줄링
            val success = alarmManager.scheduleTimeBasedAutoRun(autoRun)
            
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

