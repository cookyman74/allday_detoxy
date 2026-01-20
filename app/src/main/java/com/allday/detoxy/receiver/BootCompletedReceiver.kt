package com.allday.detoxy.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.allday.detoxy.core.manager.AutoRunAlarmManager
import com.allday.detoxy.core.manager.AutoRunGeofenceManager
import com.allday.detoxy.data.local.dao.LocationBasedAutoRunDao
import com.allday.detoxy.data.local.dao.TimeBasedAutoRunDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 부팅 완료 BroadcastReceiver
 *
 * 기기 재시작 시 모든 활성화된 자동 실행(시간 기반 + 위치 기반)을 재등록합니다.
 *
 * ## 처리 흐름
 * 1. BOOT_COMPLETED 브로드캐스트 수신
 * 2. DB에서 모든 활성화된 자동 실행 조회 (suspend 함수 사용)
 * 3. AutoRunAlarmManager.rescheduleAll() 호출하여 시간 기반 알람 재등록
 * 4. AutoRunGeofenceManager.rescheduleAll() 호출하여 위치 기반 Geofence 재등록
 *
 * ## 주의사항 (Hilt 이슈 대응)
 * - ⚠️ @AndroidEntryPoint 제거: Hilt ASM 변환 오류로 인해 수동 의존성 주입 사용
 * - EntryPointAccessors를 통해 수동으로 의존성 가져오기
 * - BroadcastReceiver는 10초 제한이 있으므로 goAsync() 사용
 * - Flow.first() 대신 suspend 함수 getAllEnabled()를 직접 호출
 *   (Flow emission 방식 변경 시 PendingResult가 닫히지 않을 위험 방지)
 * - 코루틴으로 비동기 처리 (CoroutineScope with SupervisorJob)
 *
 * @see AutoRunAlarmManager.rescheduleAll
 * @see AutoRunGeofenceManager.rescheduleAll
 * @see TimeBasedAutoRunDao.getAllEnabled
 * @see LocationBasedAutoRunDao.getAllEnabled
 */
class BootCompletedReceiver : BroadcastReceiver() {

    /**
     * Hilt EntryPoint for manual dependency injection
     *
     * @AndroidEntryPoint를 사용하지 못하는 경우 (ASM 변환 오류),
     * EntryPointAccessors를 통해 수동으로 의존성을 가져옵니다.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootCompletedReceiverEntryPoint {
        fun alarmManager(): AutoRunAlarmManager
        fun geofenceManager(): AutoRunGeofenceManager
        fun timeBasedAutoRunDao(): TimeBasedAutoRunDao
        fun locationBasedAutoRunDao(): LocationBasedAutoRunDao
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            Log.w(TAG, "⚠️ Unknown action: ${intent.action}")
            return
        }

        Log.i(TAG, "📱 Device booted, rescheduling all auto-run alarms and geofences")

        // goAsync()를 사용하여 비동기 작업 완료 보장
        val pendingResult = goAsync()

        // ⚠️ Hilt 이슈 대응: EntryPointAccessors를 통해 수동으로 의존성 가져오기
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            BootCompletedReceiverEntryPoint::class.java
        )
        val alarmManager = entryPoint.alarmManager()
        val geofenceManager = entryPoint.geofenceManager()
        val timeBasedAutoRunDao = entryPoint.timeBasedAutoRunDao()
        val locationBasedAutoRunDao = entryPoint.locationBasedAutoRunDao()

        scope.launch {
            try {
                // 1. 시간 기반 자동 실행 재등록
                // 🔧 2차 리뷰 반영: Flow.first() 대신 suspend 함수 직접 호출
                // Flow emission 방식 변경 시 PendingResult가 닫히지 않을 위험 방지
                val enabledTimeBasedAutoRuns = timeBasedAutoRunDao.getAllEnabled()
                
                if (enabledTimeBasedAutoRuns.isEmpty()) {
                    Log.d(TAG, "⏭️ No enabled time-based auto-runs to reschedule")
                } else {
                    Log.d(TAG, "🔄 Rescheduling ${enabledTimeBasedAutoRuns.size} time-based auto-runs")
                    alarmManager.rescheduleAll(enabledTimeBasedAutoRuns)
                    Log.i(TAG, "✅ Time-based auto-run alarms rescheduled successfully")
                }
                
                // 2. 위치 기반 자동 실행 재등록 (v1.1.2 개선)
                val enabledLocationBasedAutoRuns = locationBasedAutoRunDao.getAllEnabled()
                
                if (enabledLocationBasedAutoRuns.isEmpty()) {
                    Log.d(TAG, "⏭️ No enabled location-based auto-runs to reschedule")
                } else {
                    Log.d(TAG, "🔄 Rescheduling ${enabledLocationBasedAutoRuns.size} location-based auto-runs")
                    
                    // 🆕 v1.1.2: rescheduleAll 결과 처리
                    val result = geofenceManager.rescheduleAll(enabledLocationBasedAutoRuns)
                    
                    // 영구적 실패 + 스킵만 DB 비활성화 (일시적 실패는 유지)
                    val idsToDisable = result.failedPermanentIds + result.skippedIds
                    if (idsToDisable.isNotEmpty()) {
                        locationBasedAutoRunDao.disableByIds(idsToDisable)
                        Log.w(TAG, "⚠️ ${idsToDisable.size} locations disabled (permanent failure or limit)")
                    }
                    
                    // 일시적 실패는 로그만 (다음 reschedule에서 재시도)
                    if (result.failedTempIds.isNotEmpty()) {
                        Log.w(TAG, "⚠️ ${result.failedTempIds.size} locations failed temporarily, will retry later")
                    }
                    
                    Log.i(TAG, "✅ Location-based auto-run geofences rescheduled: ${result.successIds.size} success")
                }
                
                Log.i(TAG, "🎉 All auto-runs rescheduled successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to reschedule auto-runs: ${e.message}", e)
            } finally {
                // PendingResult 완료 알림
                pendingResult.finish()
            }
        }
    }
}

