package com.allday.detoxy.core.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.data.repository.UserSettingsRepository
import com.allday.detoxy.domain.repository.AutoRunSettingsRepository
import com.allday.detoxy.receiver.AutoRunAlarmReceiver
import com.allday.detoxy.worker.AutoRunWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AlarmManager 래퍼 클래스
 *
 * 시간 기반 자동 실행을 위한 알람 스케줄링을 관리합니다.
 * Android 12+ 정확 알람 권한 체크 및 PendingIntent 관리를 포함합니다.
 * 권한 없을 시 WorkManager로 자동 Fallback 처리합니다.
 *
 * ## 주요 기능
 * - **정확 알람 스케줄링**: setExactAndAllowWhileIdle() 사용 (±2분)
 * - **WorkManager Fallback**: 권한 없을 시 자동 전환 (±15분)
 * - **권한 체크**: Android 12+ SCHEDULE_EXACT_ALARM 권한 확인
 * - **Doze 모드 대응**: AllowWhileIdle 플래그로 절전 모드에서도 실행
 * - **요일별 스케줄링**: 다음 발생 시각 계산
 *
 * ## 글로벌 옵션 지원 ✅
 * - **주말 제외**: calculateNextTriggerTime()에서 excludeWeekends 설정 참조하여 토요일/일요일 건너뛰기
 * - **사전 알림**: preNotificationMinutes 설정에 따라 사전 알림 알람 추가 등록
 * 
 * @see com.allday.detoxy.domain.repository.AutoRunSettingsRepository
 * @see docs/02_advanced_autosetting_todolist.md §3.2.1
 *
 * @param context Application Context
 * @param alarmManager AlarmManager 시스템 서비스
 * @param workManager WorkManager 인스턴스
 * @param autoRunSettingsRepository 자동 실행 글로벌 설정 Repository
 * @param userSettingsRepository 사용자 설정 Repository (마스터 토글, 일시중지)
 */
@Singleton
class AutoRunAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    private val workManager: WorkManager,
    private val autoRunSettingsRepository: AutoRunSettingsRepository,
    private val userSettingsRepository: UserSettingsRepository
) {

    companion object {
        private const val TAG = "AutoRunAlarmManager"
        
        // BroadcastReceiver Intent Action
        const val ACTION_AUTO_RUN_ALARM = "com.allday.detoxy.ACTION_AUTO_RUN_ALARM"
        const val ACTION_PRE_NOTIFICATION = "com.allday.detoxy.ACTION_PRE_NOTIFICATION"
        
        // Intent Extra Keys
        const val EXTRA_AUTO_RUN_ID = "autoRunId"
        const val EXTRA_DURATION_MINUTES = "durationMinutes"
        const val EXTRA_PRESET_TYPE = "presetType"
        const val EXTRA_LABEL = "label"
        const val EXTRA_IS_PRE_NOTIFICATION = "isPreNotification"
        
        // Request Code Base (autoRunId 해시로 고유값 생성)
        private const val REQUEST_CODE_BASE = 10000
        private const val PRE_NOTIFICATION_CODE_OFFSET = 50000
        
        // 유효한 요일 코드 (대문자)
        private val VALID_DAY_CODES = setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    }

    // 정확 알람 권한 상태 (StateFlow)
    private val _canScheduleExactAlarms = MutableStateFlow(canScheduleExactAlarmsInternal())
    val canScheduleExactAlarms: StateFlow<Boolean> = _canScheduleExactAlarms.asStateFlow()

    /**
     * 정확 알람 스케줄링 가능 여부 확인 (Android 12+)
     *
     * Android 12 미만: 항상 true
     * Android 12+: SCHEDULE_EXACT_ALARM 권한 확인
     *
     * @return true: 정확 알람 사용 가능, false: WorkManager fallback 필요
     */
    fun canScheduleExactAlarms(): Boolean {
        val canSchedule = canScheduleExactAlarmsInternal()
        _canScheduleExactAlarms.value = canSchedule
        return canSchedule
    }

    private fun canScheduleExactAlarmsInternal(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Android 12 미만은 권한 불필요
        }
    }

    /**
     * 시간 기반 자동 실행 알람 스케줄링
     *
     * 다음 발생 시각을 계산하여 알람을 등록합니다.
     * - 활성화된 요일만 고려
     * - Doze 모드에서도 동작 (setExactAndAllowWhileIdle)
     * - PendingIntent.FLAG_IMMUTABLE 사용 (Android 12+ 필수)
     * - AlarmManager 실패 시 WorkManager로 자동 Fallback
     * - 글로벌 옵션: 사전 알림이 설정된 경우 별도 알람 등록
     * - 마스터 스위치 체크: 꺼져 있으면 알람 등록하지 않음
     *
     * @param autoRun 스케줄링할 시간 기반 자동 실행 설정
     * @return true: 성공, false: 실패
     */
    fun scheduleTimeBasedAutoRun(autoRun: TimeBasedAutoRun): Boolean {
        if (!autoRun.isEnabled) {
            Log.w(TAG, "⚠️ AutoRun is disabled, skipping: ${autoRun.id}")
            return false
        }

        // 마스터 스위치 체크
        val masterEnabled = runBlocking {
            userSettingsRepository.getAutoRunMasterEnabled()
        }
        if (!masterEnabled) {
            Log.w(TAG, "⚠️ AutoRun master switch is OFF, skipping: ${autoRun.id}")
            return false
        }

        // 다음 발생 시각 계산
        val nextTriggerTime = calculateNextTriggerTime(autoRun)
        if (nextTriggerTime == null) {
            Log.e(TAG, "❌ Failed to calculate next trigger time for: ${autoRun.id}")
            return false
        }

        // 글로벌 옵션: 사전 알림 시간 확인
        val preNotificationMinutes = runBlocking {
            autoRunSettingsRepository.getPreNotificationMinutes()
        }

        // 사전 알림 알람 등록 (0보다 크면)
        if (preNotificationMinutes > 0) {
            val preNotificationTime = nextTriggerTime - (preNotificationMinutes * 60 * 1000)
            if (preNotificationTime > System.currentTimeMillis()) {
                schedulePreNotification(autoRun, preNotificationTime, preNotificationMinutes)
                Log.d(TAG, "📢 Pre-notification scheduled: ${preNotificationMinutes}분 전 at ${formatTime(preNotificationTime)}")
            } else {
                Log.d(TAG, "⏭️ Pre-notification time already passed, skipping")
            }
        }

        // AlarmManager로 스케줄링 시도
        val alarmSuccess = tryScheduleWithAlarmManager(autoRun, nextTriggerTime)
        
        if (!alarmSuccess) {
            // AlarmManager 실패 시 WorkManager로 Fallback
            Log.w(TAG, "⚠️ AlarmManager failed, falling back to WorkManager for ${autoRun.id}")
            return scheduleWithWorkManager(autoRun, nextTriggerTime)
        }
        
        return true
    }
    
    /**
     * 현재 시간이 시간표 범위 내에 있을 때 즉시 타이머 시작
     * 
     * 스케줄 그룹 활성화 시 현재 시간이 시간표 범위 내에 있으면 즉시 타이머를 시작합니다.
     * WorkManager를 사용하여 Android 12+ 제한을 우회합니다.
     * 
     * 🔧 Critical Fix: 중간 진입 시 남은 시간만큼만 타이머 시작
     * - 예: 2시~6시 스케줄에서 4시 50분 진입 시 → 70분(1시간 10분)만 실행
     * - 전체 durationMinutes가 아닌 종료 시간까지의 남은 시간을 계산하여 사용
     * 
     * @param autoRun 시간 기반 자동 실행 설정
     * @return true: 타이머 시작 성공, false: 실패
     */
    fun startTimerImmediatelyIfInRange(autoRun: TimeBasedAutoRun): Boolean {
        return try {
            Log.i(TAG, "🚀 Starting timer immediately for: ${autoRun.label ?: autoRun.id}")
            
            // 🔧 남은 시간 계산 (중간 진입 시 종료 시간까지의 남은 시간만큼만 실행)
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(Calendar.MINUTE)
            val currentTimeMinutes = currentHour * 60 + currentMinute
            
            val startTimeMinutes = autoRun.hour * 60 + autoRun.minute
            val endTimeMinutes = startTimeMinutes + autoRun.durationMinutes
            val remainingMinutes = endTimeMinutes - currentTimeMinutes
            
            // 남은 시간이 1분 미만이면 시작하지 않음
            if (remainingMinutes < 1) {
                Log.w(TAG, "⚠️ Remaining time is less than 1 minute (${remainingMinutes}분), skipping timer start")
                return false
            }
            
            // 남은 시간이 전체 시간보다 크면 전체 시간 사용 (예외 상황 대비)
            val actualDurationMinutes = if (remainingMinutes > autoRun.durationMinutes) {
                Log.w(TAG, "⚠️ Remaining time (${remainingMinutes}분) is greater than total duration (${autoRun.durationMinutes}분), using total duration")
                autoRun.durationMinutes
            } else {
                remainingMinutes
            }
            
            Log.i(TAG, "⏰ Time slot: ${autoRun.hour}:${String.format("%02d", autoRun.minute)} ~ ${endTimeMinutes / 60}:${String.format("%02d", endTimeMinutes % 60)} (${autoRun.durationMinutes}분)")
            Log.i(TAG, "⏰ Current time: ${currentHour}:${String.format("%02d", currentMinute)}")
            Log.i(TAG, "⏰ Remaining time: ${actualDurationMinutes}분 (${actualDurationMinutes / 60}시간 ${actualDurationMinutes % 60}분)")
            
            // WorkManager를 사용하여 즉시 타이머 시작 (남은 시간만큼만)
            val workRequest = OneTimeWorkRequestBuilder<com.allday.detoxy.worker.AutoStartTimerWorker>()
                .setInputData(
                    workDataOf(
                        "autoRunId" to autoRun.id,
                        "durationMinutes" to actualDurationMinutes,  // 🔧 남은 시간 사용
                        "presetType" to autoRun.presetType,
                        "label" to (autoRun.label ?: ""),
                        "triggerType" to "LOCATION_IMMEDIATE",  // 위치 기반 즉시 실행
                        "isSnooze" to false
                    )
                )
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            
            workManager.enqueueUniqueWork(
                "auto_start_timer_immediate_${autoRun.id}",
                androidx.work.ExistingWorkPolicy.REPLACE,
                workRequest
            )
            
            Log.i(TAG, "✅ WorkManager job enqueued for immediate timer start: ${autoRun.label ?: autoRun.id} (${actualDurationMinutes}분)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start timer immediately: ${e.message}", e)
            false
        }
    }
    
    /**
     * 사전 알림 알람 스케줄링
     *
     * 실제 자동 실행 N분 전에 알림을 표시하기 위한 별도 알람을 등록합니다.
     * isPreNotification 플래그를 true로 설정하여 Receiver에서 구분할 수 있도록 합니다.
     *
     * ## PendingIntent 관리 (2차 리뷰 반영)
     * - **requestCode**: `abs(autoRun.id.hashCode()) + PRE_NOTIFICATION_CODE_OFFSET`
     *   - 메인 알람과 중복 방지를 위해 오프셋 추가
     *   - 동일 autoRun.id에 대해서는 항상 동일한 requestCode 생성
     * - **FLAG_UPDATE_CURRENT**: 동일 requestCode의 기존 PendingIntent를 업데이트
     *   - 동일 ID에 대해 시간/파라미터가 변경되면 최신 extras로 덮어씀
     *   - 의도된 동작: 사용자가 사전 알림 시간을 변경하면 기존 알람이 새 설정으로 교체됨
     *   - ID 재활용이 없는 한 충돌 없음 (UUID 기반 ID 사용)
     *
     * @param autoRun 자동 실행 엔티티
     * @param preNotificationTime 사전 알림 시각 (epoch millis)
     * @param minutesBefore 실제 실행 전 몇 분인지 (로그용)
     */
    private fun schedulePreNotification(autoRun: TimeBasedAutoRun, preNotificationTime: Long, minutesBefore: Int) {
        val intent = Intent(context, AutoRunAlarmReceiver::class.java).apply {
            action = ACTION_PRE_NOTIFICATION
            putExtra(EXTRA_AUTO_RUN_ID, autoRun.id)
            putExtra(EXTRA_DURATION_MINUTES, autoRun.durationMinutes)
            putExtra(EXTRA_PRESET_TYPE, autoRun.presetType)
            putExtra(EXTRA_LABEL, autoRun.label)
            putExtra(EXTRA_IS_PRE_NOTIFICATION, true)
        }

        val requestCode = kotlin.math.abs(autoRun.id.hashCode()) + PRE_NOTIFICATION_CODE_OFFSET
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    preNotificationTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    preNotificationTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "✅ Pre-notification alarm set for ${autoRun.id}: ${minutesBefore}분 전")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set pre-notification alarm: ${e.message}")
        }
    }
    
    /**
     * AlarmManager로 알람 스케줄링 시도
     *
     * 성공 시 이전에 스케줄링된 WorkManager 작업이 있다면 취소합니다.
     * (권한 복구 시 중복 트리거 방지)
     *
     * @return true: 성공, false: 실패 (WorkManager fallback 필요)
     */
    private fun tryScheduleWithAlarmManager(autoRun: TimeBasedAutoRun, nextTriggerTime: Long): Boolean {
        // PendingIntent 생성
        val pendingIntent = createPendingIntent(autoRun)
        
        return try {
            if (canScheduleExactAlarms()) {
                // 정확 알람 사용 (±2분 정확도)
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
                
                // 중요: 이전에 WorkManager로 스케줄링된 작업이 있다면 취소
                // (권한 없을 때 WorkManager로 스케줄링 → 권한 복구 시 AlarmManager로 전환)
                // 취소하지 않으면 AlarmManager + WorkManager 둘 다 트리거되어 중복 실행됨
                try {
                    val workName = "${AutoRunWorker.WORK_NAME_PREFIX}${autoRun.id}"
                    workManager.cancelUniqueWork(workName)
                    Log.d(TAG, "🗑️ Cancelled previous WorkManager task (if any) for ${autoRun.id}")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to cancel WorkManager task: ${e.message}")
                }
                
                Log.i(TAG, "✅ Exact alarm scheduled for ${autoRun.label ?: autoRun.id} at ${formatTime(nextTriggerTime)}")
                true
            } else {
                // 정확 알람 권한 없음 → WorkManager로 fallback
                Log.w(TAG, "⚠️ No exact alarm permission, will use WorkManager fallback")
                false
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException while scheduling alarm: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception while scheduling alarm: ${e.message}")
            false
        }
    }
    
    /**
     * WorkManager로 알람 스케줄링 (Fallback)
     *
     * AlarmManager 실패 시 대체 수단으로 사용됩니다.
     * 정확도: ±15분 (AlarmManager의 ±2분 대비 떨어짐)
     *
     * @param autoRun 스케줄링할 자동 실행 설정
     * @param nextTriggerTime 다음 트리거 시각 (epoch millis)
     * @return true: 성공, false: 실패
     */
    private fun scheduleWithWorkManager(autoRun: TimeBasedAutoRun, nextTriggerTime: Long): Boolean {
        return try {
            // 현재 시각부터 트리거 시각까지의 지연 시간 계산
            val now = System.currentTimeMillis()
            val delayMillis = (nextTriggerTime - now).coerceAtLeast(0)
            
            // WorkRequest Input Data 구성
            val inputData = Data.Builder()
                .putString(AutoRunWorker.KEY_AUTO_RUN_ID, autoRun.id)
                .putInt(AutoRunWorker.KEY_DURATION_MINUTES, autoRun.durationMinutes)
                .putString(AutoRunWorker.KEY_PRESET_TYPE, autoRun.presetType)
                .putString(AutoRunWorker.KEY_LABEL, autoRun.label)
                .build()
            
            // OneTimeWorkRequest 생성
            val workRequest = OneTimeWorkRequestBuilder<AutoRunWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build()
            
            // WorkManager에 enqueue (고유 이름으로 중복 방지)
            val workName = "${AutoRunWorker.WORK_NAME_PREFIX}${autoRun.id}"
            workManager.enqueueUniqueWork(
                workName,
                androidx.work.ExistingWorkPolicy.REPLACE,
                workRequest
            )
            
            Log.i(TAG, "✅ WorkManager scheduled for ${autoRun.label ?: autoRun.id} at ${formatTime(nextTriggerTime)} (±15분 오차)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to schedule with WorkManager: ${e.message}", e)
            false
        }
    }

    /**
     * 시간 기반 자동 실행 알람 취소
     *
     * AlarmManager와 WorkManager 모두에서 취소 시도합니다.
     *
     * @param autoRunId 취소할 자동 실행 ID
     */
    fun cancelTimeBasedAutoRun(autoRunId: String) {
        // 🔧 Critical Fix: 메인 알람과 사전 알림 알람 모두 취소
        
        // 1. 메인 알람 취소
        try {
            val pendingIntent = createPendingIntentById(autoRunId)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "🗑️ Main alarm canceled for autoRunId: $autoRunId")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to cancel main alarm: ${e.message}")
        }
        
        // 2. 🔧 사전 알림 알람 취소 (Critical Fix)
        // 비활성화/삭제 시 사전 알림 알람도 함께 취소하지 않으면
        // 사용자가 "N분 전" 알림을 계속 받게 됨
        try {
            val preNotificationIntent = createPreNotificationPendingIntentById(autoRunId)
            alarmManager.cancel(preNotificationIntent)
            preNotificationIntent.cancel()
            Log.d(TAG, "🗑️ Pre-notification alarm canceled for autoRunId: $autoRunId")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to cancel pre-notification alarm: ${e.message}")
        }
        
        // 3. WorkManager 취소 (혹시 WorkManager로 스케줄링되어 있을 수 있으므로)
        try {
            val workName = "${AutoRunWorker.WORK_NAME_PREFIX}$autoRunId"
            workManager.cancelUniqueWork(workName)
            Log.d(TAG, "🗑️ WorkManager canceled for autoRunId: $autoRunId")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to cancel WorkManager: ${e.message}")
        }
        
        Log.i(TAG, "✅ AutoRun canceled (main + pre-notification + WorkManager): $autoRunId")
    }

    /**
     * 모든 시간 기반 자동 실행 알람 재스케줄링
     *
     * 앱 재시작 또는 설정 변경 시 호출됩니다.
     *
     * @param autoRuns 재스케줄링할 자동 실행 리스트
     */
    fun rescheduleAll(autoRuns: List<TimeBasedAutoRun>) {
        Log.i(TAG, "🔄 Rescheduling ${autoRuns.size} auto-run alarms")
        
        var successCount = 0
        var failCount = 0
        
        autoRuns.forEach { autoRun ->
            if (scheduleTimeBasedAutoRun(autoRun)) {
                successCount++
            } else {
                failCount++
            }
        }
        
        Log.i(TAG, "✅ Rescheduling complete - Success: $successCount, Failed: $failCount")
    }

    /**
     * 다음 트리거 시각 계산
     *
     * 활성화된 요일 중 가장 가까운 시각을 반환합니다.
     * 재스케줄 시 다음 주 같은 요일을 놓치지 않도록 최대 13일(2주) 검색합니다.
     *
     * ## 글로벌 옵션 적용
     * - **주말 제외**: excludeWeekends 설정이 true이면 토요일/일요일 건너뛰기
     *
     * @param autoRun 시간 기반 자동 실행 설정
     * @return 다음 트리거 시각 (epoch millis), null: 계산 실패
     */
    private fun calculateNextTriggerTime(autoRun: TimeBasedAutoRun): Long? {
        val enabledDays = parseEnabledDays(autoRun.enabledDays)
        if (enabledDays.isEmpty()) {
            Log.w(TAG, "⚠️ No enabled days for autoRun: ${autoRun.id}")
            return null
        }

        // 글로벌 옵션: 주말 제외 설정 확인
        val excludeWeekends = runBlocking {
            autoRunSettingsRepository.getExcludeWeekends()
        }

        val now = Calendar.getInstance()
        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, autoRun.hour)
            set(Calendar.MINUTE, autoRun.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 오늘부터 최대 13일(2주) 검색
        // 이유: 오늘 같은 요일인데 시간이 지난 경우 다음 주 같은 요일(7일 후)을 찾기 위함
        // 예: 월요일 오후에 월요일 오전 알람 재스케줄 → 다음 주 월요일 찾아야 함
        for (daysToAdd in 0..13) {
            val checkCalendar = targetCalendar.clone() as Calendar
            checkCalendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
            
            // 현재 시각 이후만 허용 (재스케줄 시 현재 시각은 제외)
            // 1초 마진 추가로 정확히 같은 시각의 경우도 안전하게 처리
            if (checkCalendar.timeInMillis <= now.timeInMillis) {
                continue
            }
            
            // 요일 확인
            val dayOfWeek = getDayOfWeekString(checkCalendar)
            
            // 주말 제외 옵션 적용
            if (excludeWeekends && (dayOfWeek == "SAT" || dayOfWeek == "SUN")) {
                Log.d(TAG, "⏭️ Skipping weekend: $dayOfWeek (excludeWeekends enabled)")
                continue
            }
            
            if (enabledDays.contains(dayOfWeek)) {
                val excludeInfo = if (excludeWeekends) " [주말 제외 ON]" else ""
                Log.d(TAG, "✅ Next trigger found: ${formatTime(checkCalendar.timeInMillis)} ($dayOfWeek) for ${autoRun.label ?: autoRun.id}$excludeInfo")
                return checkCalendar.timeInMillis
            }
        }

        Log.w(TAG, "⚠️ No matching day found in next 14 days for autoRun: ${autoRun.id}")
        return null
    }

    /**
     * enabledDays JSON 파싱
     *
     * JSON 배열에서 요일 코드를 추출합니다.
     * 대소문자를 무시하고 uppercase로 정규화합니다.
     *
     * @param enabledDaysJson JSON 문자열 (예: "[\"MON\",\"TUE\",\"WED\"]")
     * @return 요일 코드 Set (예: setOf("MON", "TUE", "WED"))
     */
    private fun parseEnabledDays(enabledDaysJson: String): Set<String> {
        if (enabledDaysJson.isBlank() || enabledDaysJson == "[]") {
            Log.w(TAG, "⚠️ Empty enabledDays JSON")
            return emptySet()
        }
        
        return try {
            // 간단한 JSON 파싱 (정규식 사용)
            val regex = Regex(""""(\w+)"""")
            regex.findAll(enabledDaysJson)
                .map { it.groupValues[1].uppercase() } // 대소문자 정규화
                .filter { it in VALID_DAY_CODES } // 유효한 요일만 필터링
                .toSet()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to parse enabledDays: $enabledDaysJson", e)
            emptySet()
        }
    }

    /**
     * Calendar 요일 → 문자열 변환
     *
     * @param calendar Calendar 인스턴스
     * @return 요일 코드 (MON, TUE, WED, THU, FRI, SAT, SUN)
     */
    private fun getDayOfWeekString(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MON"
            Calendar.TUESDAY -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY -> "THU"
            Calendar.FRIDAY -> "FRI"
            Calendar.SATURDAY -> "SAT"
            Calendar.SUNDAY -> "SUN"
            else -> "UNKNOWN"
        }
    }

    /**
     * PendingIntent 생성
     *
     * BroadcastReceiver를 타깃으로 하는 PendingIntent를 생성합니다.
     * FLAG_IMMUTABLE 사용 (Android 12+ 필수)
     *
     * @param autoRun 시간 기반 자동 실행 설정
     * @return PendingIntent
     */
    private fun createPendingIntent(autoRun: TimeBasedAutoRun): PendingIntent {
        val intent = Intent(context, AutoRunAlarmReceiver::class.java).apply {
            action = ACTION_AUTO_RUN_ALARM
            putExtra(EXTRA_AUTO_RUN_ID, autoRun.id)
            putExtra(EXTRA_DURATION_MINUTES, autoRun.durationMinutes)
            putExtra(EXTRA_PRESET_TYPE, autoRun.presetType)
            putExtra(EXTRA_LABEL, autoRun.label)
        }

        // requestCode 생성 (음수 hashCode 대응)
        val requestCode = kotlin.math.abs(autoRun.id.hashCode()) % 10000 + REQUEST_CODE_BASE
        
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * ID로 PendingIntent 생성 (취소용)
     *
     * @param autoRunId 자동 실행 ID
     * @return PendingIntent
     */
    private fun createPendingIntentById(autoRunId: String): PendingIntent {
        val intent = Intent(context, AutoRunAlarmReceiver::class.java).apply {
            action = ACTION_AUTO_RUN_ALARM
            putExtra(EXTRA_AUTO_RUN_ID, autoRunId)
        }

        // requestCode 생성 (음수 hashCode 대응)
        val requestCode = kotlin.math.abs(autoRunId.hashCode()) % 10000 + REQUEST_CODE_BASE
        
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * ID로 사전 알림 PendingIntent 생성 (취소용)
     *
     * 사전 알림 알람을 취소할 때 사용하는 PendingIntent를 생성합니다.
     * requestCode가 메인 알람과 달라 별도 메서드로 분리했습니다.
     *
     * @param autoRunId 자동 실행 ID
     * @return PendingIntent for pre-notification alarm
     */
    private fun createPreNotificationPendingIntentById(autoRunId: String): PendingIntent {
        val intent = Intent(context, AutoRunAlarmReceiver::class.java).apply {
            action = ACTION_PRE_NOTIFICATION
            putExtra(EXTRA_AUTO_RUN_ID, autoRunId)
        }

        // 사전 알림용 requestCode (메인 알람과 중복 방지)
        val requestCode = kotlin.math.abs(autoRunId.hashCode()) + PRE_NOTIFICATION_CODE_OFFSET
        
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 시각 포맷팅 (로그용)
     *
     * @param timeInMillis epoch millis
     * @return 포맷된 시각 문자열 (예: "2025-10-22 14:30:00")
     */
    private fun formatTime(timeInMillis: Long): String {
        val calendar = Calendar.getInstance().apply {
            this.timeInMillis = timeInMillis
        }
        return String.format(
            "%04d-%02d-%02d %02d:%02d:%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND)
        )
    }
}

