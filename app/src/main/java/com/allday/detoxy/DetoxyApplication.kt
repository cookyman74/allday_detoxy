package com.allday.detoxy

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.allday.detoxy.core.manager.AutoRunAlarmManager
import com.allday.detoxy.core.manager.AutoRunGeofenceManager
import com.allday.detoxy.core.manager.GrayscaleManager
import com.allday.detoxy.core.utils.AnalyticsHelper
import com.allday.detoxy.data.local.dao.LocationBasedAutoRunDao
import com.allday.detoxy.data.local.dao.TimeBasedAutoRunDao
import com.allday.detoxy.domain.model.FocusState
import com.allday.detoxy.domain.repository.FocusSettingsRepository
import com.allday.detoxy.service.timer.FocusTimerService
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hilt 의존성 주입을 위한 Application 클래스
 *
 * @HiltAndroidApp 어노테이션으로 Hilt의 코드 생성 트리거
 * 앱의 전체 생명주기 동안 DI 컨테이너 유지
 *
 * Firebase 서비스 초기화:
 * - Crashlytics: google-services 플러그인에 의해 자동 초기화
 * - Analytics: AnalyticsHelper.initialize()로 명시적 초기화
 *
 * WorkManager 설정:
 * - Configuration.Provider 구현으로 HiltWorkerFactory 제공
 * - WorkManager는 자동 초기화되며 이 설정을 사용함
 * - @HiltWorker 어노테이션을 통한 Worker 의존성 주입 지원
 *
 * 🆕 앱 시작 시 자동 실행 재등록:
 * - 앱이 종료된 후 다시 시작될 때 자동 실행(알람, Geofence) 재등록
 * - 배터리 최적화로 인한 알람/Geofence 손실 대응
 */
@HiltAndroidApp
class DetoxyApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    /**
     * Hilt EntryPoint for manual dependency injection
     *
     * Application.onCreate()에서는 @Inject가 작동하지 않으므로
     * EntryPoint를 사용하여 수동으로 의존성을 가져옵니다.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DetoxyApplicationEntryPoint {
        fun alarmManager(): AutoRunAlarmManager
        fun geofenceManager(): AutoRunGeofenceManager
        fun timeBasedAutoRunDao(): TimeBasedAutoRunDao
        fun locationBasedAutoRunDao(): LocationBasedAutoRunDao
        // 흑백 모드 상태 복원용 (Phase 7)
        fun grayscaleManager(): GrayscaleManager
        fun focusSettingsRepository(): FocusSettingsRepository
    }
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val TAG = "DetoxyApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Firebase Analytics 초기화 (Week 1 Task 2.3)
        AnalyticsHelper.initialize(this)
        Log.i(TAG, "✅ AnalyticsHelper initialized")
        
        // Firebase Crashlytics는 자동 초기화됨 (google-services.json 필요)
        // 필요 시 커스텀 키/로그 설정:
        // FirebaseCrashlytics.getInstance().setCustomKey("key", "value")
        // FirebaseCrashlytics.getInstance().log("App started")
        
        // WorkManager는 자동으로 초기화되며 workManagerConfiguration을 사용함
        Log.i(TAG, "✅ WorkManager with HiltWorkerFactory will be initialized automatically")
        
        // 🆕 앱 시작 시 자동 실행 재등록 (앱이 종료된 후 다시 시작될 때)
        rescheduleAutoRunsOnAppStart()
        
        // 🆕 흑백 모드 상태 복원 (Phase 7)
        restoreGrayscaleStateOnAppStart()
    }
    
    /**
     * 앱 시작 시 자동 실행 재등록
     *
     * 앱이 종료된 후 다시 시작될 때 자동 실행(알람, Geofence)을 재등록합니다.
     * 배터리 최적화나 시스템에 의한 프로세스 종료로 인한 알람/Geofence 손실을 방지합니다.
     *
     * 주의: 이 메서드는 Application.onCreate()에서 호출되므로
     * EntryPoint를 사용하여 수동으로 의존성을 주입해야 합니다.
     * Hilt 초기화를 기다리기 위해 지연 실행합니다.
     */
    private fun rescheduleAutoRunsOnAppStart() {
        Log.d(TAG, "🔄 rescheduleAutoRunsOnAppStart() called")
        applicationScope.launch {
            try {
                // 🆕 Hilt 초기화를 기다리기 위해 짧은 지연 (EntryPoint 사용 가능할 때까지)
                kotlinx.coroutines.delay(500)
                
                Log.d(TAG, "🔄 Attempting to get EntryPoint...")
                // EntryPoint를 통해 의존성 가져오기
                val entryPoint = EntryPointAccessors.fromApplication(
                    this@DetoxyApplication,
                    DetoxyApplicationEntryPoint::class.java
                )
                Log.d(TAG, "✅ EntryPoint obtained successfully")
                
                val alarmManager = entryPoint.alarmManager()
                val geofenceManager = entryPoint.geofenceManager()
                val timeBasedAutoRunDao = entryPoint.timeBasedAutoRunDao()
                val locationBasedAutoRunDao = entryPoint.locationBasedAutoRunDao()
                
                Log.d(TAG, "🔄 Checking auto-runs on app start...")
                
                // 1. 시간 기반 자동 실행 재등록
                val enabledTimeBasedAutoRuns = timeBasedAutoRunDao.getAllEnabled()
                Log.d(TAG, "   Found ${enabledTimeBasedAutoRuns.size} enabled time-based auto-runs")
                if (enabledTimeBasedAutoRuns.isNotEmpty()) {
                    Log.d(TAG, "🔄 Rescheduling ${enabledTimeBasedAutoRuns.size} time-based auto-runs on app start")
                    alarmManager.rescheduleAll(enabledTimeBasedAutoRuns)
                    Log.i(TAG, "✅ Time-based auto-runs rescheduled on app start")
                } else {
                    Log.d(TAG, "ℹ️ No enabled time-based auto-runs to reschedule")
                }
                
                // 2. 위치 기반 자동 실행 재등록
                val enabledLocationBasedAutoRuns = locationBasedAutoRunDao.getAllEnabled()
                Log.d(TAG, "   Found ${enabledLocationBasedAutoRuns.size} enabled location-based auto-runs")
                if (enabledLocationBasedAutoRuns.isNotEmpty()) {
                    Log.d(TAG, "🔄 Rescheduling ${enabledLocationBasedAutoRuns.size} location-based auto-runs on app start")
                    geofenceManager.rescheduleAll(enabledLocationBasedAutoRuns)
                    Log.i(TAG, "✅ Location-based auto-runs rescheduled on app start")
                } else {
                    Log.d(TAG, "ℹ️ No enabled location-based auto-runs to reschedule")
                }
                
                Log.i(TAG, "✅ Auto-runs rescheduled on app start completed")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to reschedule auto-runs on app start: ${e.message}", e)
                e.printStackTrace()
                // 실패해도 앱은 정상 작동 (다음 부팅 시 BootCompletedReceiver가 재등록)
            }
        }
    }
    
    /**
     * 앱 시작 시 흑백 모드 상태 복원 (Phase 7)
     *
     * 1) 룰 ID 복원 (DataStore에서)
     * 2) 룰 정합성 검증 (시스템에서 삭제됐는지)
     * 3) 집중 모드 진행 중 + 설정 ON → 재적용
     */
    private fun restoreGrayscaleStateOnAppStart() {
        Log.d(TAG, "⚫ restoreGrayscaleStateOnAppStart() called")
        applicationScope.launch {
            try {
                // Hilt 초기화를 기다리기 위해 짧은 지연
                kotlinx.coroutines.delay(600)
                
                val entryPoint = EntryPointAccessors.fromApplication(
                    this@DetoxyApplication,
                    DetoxyApplicationEntryPoint::class.java
                )
                val grayscaleManager = entryPoint.grayscaleManager()
                val settingsRepository = entryPoint.focusSettingsRepository()
                
                // 1) 룰 ID 복원
                grayscaleManager.restoreRuleId()
                Log.d(TAG, "✅ Grayscale rule ID restored")
                
                // 2) 룰 정합성 검증 (시스템에서 삭제됐는지)
                // ⚠️ 핫픽스 v3: 이미 활성화 상태면 validate 건너뛰기 (레이스 컨디션 방지)
                if (!grayscaleManager.isActive()) {
                    grayscaleManager.validateRuleExists()
                    Log.d(TAG, "✅ Grayscale rule validated")
                } else {
                    Log.d(TAG, "⚠️ Skipping validateRuleExists - already active")
                }
                
                // 3) 집중 모드 진행 중 + 설정 ON → 재적용
                val isFocusActive = FocusTimerService.state.value == FocusState.RUNNING
                val isGrayscaleEnabled = settingsRepository.grayscaleModeEnabledFlow.first()
                
                Log.d(TAG, "   isFocusActive=$isFocusActive, isGrayscaleEnabled=$isGrayscaleEnabled")
                
                if (isFocusActive && isGrayscaleEnabled) {
                    val result = grayscaleManager.enableGrayscaleIfNeeded()
                    Log.i(TAG, "✅ Grayscale mode re-applied on app start: result=$result")
                } else {
                    Log.d(TAG, "ℹ️ Grayscale re-apply skipped: focus=${isFocusActive}, enabled=${isGrayscaleEnabled}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to restore grayscale state: ${e.message}", e)
            }
        }
    }
    
    /**
     * WorkManager Configuration 제공
     *
     * WorkManager가 자동 초기화될 때 이 설정을 사용합니다.
     * HiltWorkerFactory를 통해 Worker에서 Hilt 의존성 주입이 가능합니다.
     *
     * @return WorkManager Configuration with HiltWorkerFactory
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
