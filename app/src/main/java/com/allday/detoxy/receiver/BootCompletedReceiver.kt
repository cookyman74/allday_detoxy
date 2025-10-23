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
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 부팅 완료 BroadcastReceiver
 *
 * 기기 재시작 시 모든 활성화된 시간 기반 자동 실행 알람을 재등록합니다.
 *
 * ## 처리 흐름
 * 1. BOOT_COMPLETED 브로드캐스트 수신
 * 2. DB에서 모든 활성화된 자동 실행 조회 (suspend 함수 사용)
 * 3. AutoRunAlarmManager.rescheduleAll() 호출하여 알람 재등록
 *
 * ## 주의사항 (2차 리뷰 반영)
 * - Hilt 의존성 주입을 사용하므로 @AndroidEntryPoint 필수
 * - BroadcastReceiver는 10초 제한이 있으므로 goAsync() 사용
 * - Flow.first() 대신 suspend 함수 getAllEnabled()를 직접 호출
 *   (Flow emission 방식 변경 시 PendingResult가 닫히지 않을 위험 방지)
 * - 코루틴으로 비동기 처리 (CoroutineScope with SupervisorJob)
 *
 * @see AutoRunAlarmManager.rescheduleAll
 * @see TimeBasedAutoRunDao.getAllEnabled
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    @Inject
    lateinit var alarmManager: AutoRunAlarmManager

    @Inject
    lateinit var timeBasedAutoRunDao: TimeBasedAutoRunDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            Log.w(TAG, "⚠️ Unknown action: ${intent.action}")
            return
        }

        Log.i(TAG, "📱 Device booted, rescheduling all auto-run alarms")

        // goAsync()를 사용하여 비동기 작업 완료 보장
        val pendingResult = goAsync()

        scope.launch {
            try {
                // 🔧 2차 리뷰 반영: Flow.first() 대신 suspend 함수 직접 호출
                // Flow emission 방식 변경 시 PendingResult가 닫히지 않을 위험 방지
                val enabledAutoRuns = timeBasedAutoRunDao.getAllEnabled()
                
                if (enabledAutoRuns.isEmpty()) {
                    Log.d(TAG, "⏭️ No enabled auto-runs to reschedule")
                    return@launch
                }

                Log.d(TAG, "🔄 Rescheduling ${enabledAutoRuns.size} enabled auto-runs")
                
                // 모든 알람 재등록
                alarmManager.rescheduleAll(enabledAutoRuns)
                
                Log.i(TAG, "✅ All auto-run alarms rescheduled successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to reschedule alarms: ${e.message}", e)
            } finally {
                // PendingResult 완료 알림
                pendingResult.finish()
            }
        }
    }
}

