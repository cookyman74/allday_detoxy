package com.allday.detoxy.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.allday.detoxy.core.manager.AutoRunNotificationManager
import com.allday.detoxy.data.local.entity.FocusSession
import com.allday.detoxy.domain.model.FocusState
import com.allday.detoxy.domain.repository.FocusRepository
import com.allday.detoxy.service.timer.FocusTimerService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID

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
    private val notificationManager: AutoRunNotificationManager,
    private val focusRepository: FocusRepository  // 🆕 sessionId 생성 및 FocusSession 저장용
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
                // 🔧 Critical Fix: setForeground() 제거
                // FocusTimerService가 자체적으로 포그라운드 서비스이므로 Worker를 포그라운드로 만들 필요 없음
                // setForeground() 호출이 FocusTimerService의 startForeground()와 충돌하여 서비스가 종료되는 문제 해결
                
                // 🆕 이미 타이머가 실행 중인지 확인 (중복 세션 생성 방지)
                val isTimerRunning = FocusTimerService.state.value == FocusState.RUNNING
                val existingSessionId = FocusTimerService.currentSessionId.value
                
                val sessionId: String
                if (isTimerRunning && existingSessionId != null) {
                    // 이미 타이머 실행 중 → 기존 세션 재사용
                    sessionId = existingSessionId
                    Log.d(TAG, "⚠️ Timer already running, reusing existing sessionId: $sessionId")
                } else {
                    // 새로운 타이머 시작 → 새 세션 생성
                    sessionId = UUID.randomUUID().toString()
                    try {
                        focusRepository.startSession(
                            FocusSession(
                                id = sessionId,
                                startTime = System.currentTimeMillis(),
                                endTime = null,
                                durationMinutes = durationMinutes,
                                success = false
                            )
                        )
                        Log.d(TAG, "✅ FocusSession created: sessionId=$sessionId, duration=$durationMinutes")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to create FocusSession: ${e.message}", e)
                        // 세션 생성 실패해도 타이머는 시작 (통계는 저장되지 않을 수 있음)
                    }
                }
                
                // 타이머 시작
                Log.d(TAG, "🚀 Preparing to start FocusTimerService...")
                val intent = Intent(applicationContext, FocusTimerService::class.java).apply {
                    action = FocusTimerService.ACTION_START
                    putExtra(FocusTimerService.EXTRA_DURATION_MINUTES, durationMinutes)
                    putExtra(FocusTimerService.EXTRA_SESSION_ID, sessionId)  // 🆕 sessionId 전달
                    putExtra(FocusTimerService.EXTRA_PRESET_TYPE, presetType ?: "STANDARD")
                    putExtra(FocusTimerService.EXTRA_AUTO_RUN_ID, autoRunId)
                    putExtra(FocusTimerService.EXTRA_AUTO_RUN_LABEL, label)
                }
                Log.d(TAG, "📦 Intent created: action=${intent.action}, duration=$durationMinutes, autoRunId=$autoRunId, sessionId=$sessionId")

                try {
                    Log.d(TAG, "📞 Calling startForegroundService()...")
                    applicationContext.startForegroundService(intent)
                    Log.d(TAG, "✅ startForegroundService() called successfully")
                    
                    // 🆕 서비스가 onStartCommand()를 호출하고 startForeground()를 완료할 시간을 주기 위해 딜레이
                    // Android 시스템이 startForeground() 호출 전에 서비스를 종료하지 않도록 보호
                    kotlinx.coroutines.delay(500) // 500ms로 증가하여 서비스가 완전히 시작되도록 보장
                    Log.d(TAG, "⏱️ Delay completed, service should have started")
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "❌ IllegalStateException: ${e.message}", e)
                    // Android 12+ 백그라운드 제한으로 인한 실패일 수 있음
                    // 일반 startService()로 재시도 (권장되지 않지만 fallback)
                    try {
                        Log.w(TAG, "⚠️ Attempting fallback: startService()")
                        applicationContext.startService(intent)
                        Log.d(TAG, "✅ Fallback startService() called")
                        kotlinx.coroutines.delay(500) // fallback에서도 딜레이
                    } catch (e2: Exception) {
                        Log.e(TAG, "❌ Fallback also failed: ${e2.message}", e2)
                        throw e2
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to start service: ${e.message}", e)
                    throw e
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start timer: ${e.message}", e)
            return Result.failure()
        }
    }
}

