package com.allday.detoxy

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.allday.detoxy.core.utils.AnalyticsHelper
import dagger.hilt.android.HiltAndroidApp
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
 * - HiltWorkerFactory를 통한 의존성 주입 지원
 */
@HiltAndroidApp
class DetoxyApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
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
        
        // WorkManager는 getWorkManagerConfiguration()으로 초기화됨
        Log.i(TAG, "✅ WorkManager with HiltWorkerFactory configured")
    }
    
    /**
     * WorkManager Configuration 제공
     *
     * Hilt를 통한 Worker 의존성 주입을 위해 HiltWorkerFactory 사용
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
