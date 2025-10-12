package com.allday.detoxy

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt 의존성 주입을 위한 Application 클래스
 *
 * @HiltAndroidApp 어노테이션으로 Hilt의 코드 생성 트리거
 * 앱의 전체 생명주기 동안 DI 컨테이너 유지
 *
 * Firebase Crashlytics는 google-services 플러그인에 의해 자동으로 초기화됩니다.
 * 명시적인 초기화 코드가 필요하지 않습니다.
 */
@HiltAndroidApp
class DetoxyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Firebase Crashlytics는 자동 초기화됨 (google-services.json 필요)
        // 필요 시 커스텀 키/로그 설정:
        // FirebaseCrashlytics.getInstance().setCustomKey("key", "value")
        // FirebaseCrashlytics.getInstance().log("App started")
        
        // TODO: Timber 로그 초기화 (추후 추가)
    }
}
